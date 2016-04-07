package com.github.putpixel.gitfileloader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FS;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Provides convenient API to load single or multiple files from remote GIT
 * repository using JGIT without checking out rest of the files
 *
 * @author putpixel
 *
 */
public class GitLoader implements AutoCloseable {

    private boolean initialized;

    private GitParams setup;

    private String localPath;

    private FileRepository repo;

    public GitLoader(GitParams setup) {
        this.setup = setup;
        try {
            String postfix = Optional.ofNullable(setup.getPostfix()).map(it -> "-" + it).orElse("");
            Path localDir = Files.createTempDirectory("git-loader" + postfix);
            localPath = localDir.toString();
            repo = new FileRepository(localPath + "/.git");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        deleteLocalRepository();
    }

    public void deleteLocalRepository() {
        repo.close();
        new File(localPath).delete();
    }

    public String getLocalPath() {
        return localPath;
    }

    private void assertInitialized() {
        Preconditions.checkState(initialized, "repository already initialized, please make sure you called 'cloneRemoteRepository' method");
    }

    /**
     * This methods used to initialize local repository in temporary folder, this folder will be used to checkout files. As soon as loader is closed this folder
     * will be deleted
     */
    public void cloneRemoteRepository() {
        Preconditions.checkState(!initialized, "repository already initialized");
        try {
            Git.cloneRepository()
                    .setURI(setup.getRepositoryUrl())
                    .setCredentialsProvider(createCredentials())
                    .setNoCheckout(true)
                    .setDirectory(new File(localPath))
                    .call();
            initialized = true;
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    private UsernamePasswordCredentialsProvider createCredentials() {
        if (setup.getPrivateKeyPath() != null) {
            SshSessionFactory.setInstance(new JschConfigSessionFactory() {
                @Override
                protected void configure(OpenSshConfig.Host host, Session session) {
                    session.setConfig("StrictHostKeyChecking", "false");
                    session.setPassword(setup.getPassword());
                }

                @Override
                protected JSch createDefaultJSch(FS fs) throws JSchException {
                    JSch defaultJSch = super.createDefaultJSch(fs);
                    defaultJSch.addIdentity(setup.getPrivateKeyPath(), setup.getPassword());
                    return defaultJSch;
                }
            });
            return null;
        }
        else {
            return new UsernamePasswordCredentialsProvider(setup.getLogin(), setup.getPassword());
        }
    }

    /**
     * Checkout single file from remote repository and returns file's content as string
     *
     * @param filePathInRemoteRepo
     * @return file's content as string
     */
    public String checkoutAndGetFileContent(String filePathInRemoteRepo) {
        try {
            return com.google.common.io.Files.toString(checkoutFile(filePathInRemoteRepo), Charset.defaultCharset());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private File buildFileInLocalRepo(String filePathInRemoteRepo) {
        return new File(localPath + "/" + filePathInRemoteRepo);
    }

    /**
     * List all files in remote master with they object ids
     *
     */
    public List<FileInRemoteRepository> listFilesInRemoteMaster() {
        try {
            return doListFiles("refs/remotes/origin/master");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * List all files by given reference with they object ids
     *
     */
    public List<FileInRemoteRepository> listFilesByRef(String ref) {
        try {
            return doListFiles(ref);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<FileInRemoteRepository> doListFiles(String pathRef) throws Exception {
        assertInitialized();
        Preconditions.checkNotNull(pathRef);

        List<FileInRemoteRepository> result = new ArrayList<>();
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit parseCommit = walk.parseCommit(repo.resolve(pathRef));
            RevTree tree = parseCommit.getTree();

            try (TreeWalk treeWalk = new TreeWalk(repo)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                while (treeWalk.next()) {
                    FileInRemoteRepository fileInRemoteRepository = new FileInRemoteRepository(treeWalk.getObjectId(0).getName(), treeWalk.getPathString());
                    result.add(fileInRemoteRepository);
                }

                return result;
            }
        }
    }

    private void doCheckout(Collection<String> pathsInRemoteRepo) throws Exception {
        assertInitialized();
        Ref headRef = repo.findRef(Constants.HEAD);
        ObjectId branch = repo.resolve(setup.getRemoteBranch());
        RevCommit headCommit = null;
        RevCommit newCommit = null;
        try (RevWalk revWalk = new RevWalk(repo)) {
            AnyObjectId headId = headRef.getObjectId();
            headCommit = headId == null ? null : revWalk.parseCommit(headId);
            newCommit = revWalk.parseCommit(branch);
        }
        RevTree headTree = headCommit == null ? null : headCommit.getTree();
        DirCacheCheckout dco;
        DirCache dc = repo.lockDirCache();
        try {
            dco = new DirCacheCheckout(repo, headTree, dc, newCommit.getTree()) {
                @Override
                public void prescanOneTree() throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
                    super.prescanOneTree();
                    Set<String> unknownPathes = pathsInRemoteRepo.stream()
                            .filter(it -> !getUpdated().containsKey(it))
                            .collect(Collectors.toSet());
                    Preconditions.checkState(unknownPathes.isEmpty(), "Path(s) not found in remote repo: " + Joiner.on("\n").join(unknownPathes));

                    Map<String, String> newUpdated = new HashMap<>();
                    // Nulls are possible, collectors can't be used :(
                    pathsInRemoteRepo.stream().forEach(it -> newUpdated.put(it, getUpdated().get(it)));

                    getUpdated().clear();
                    getUpdated().putAll(newUpdated);
                }
            };
            dco.setFailOnConflict(true);
            dco.checkout();
        } finally {
            dc.unlock();
        }
    }

    /**
     * Checkout single file from remote repository and returns file in local one. As soon as loader closed file will be deleted
     *
     * @param pathInRemoteRepo
     * @return file in local repository
     */
    public File checkoutFile(String pathInRemoteRepo) {
        Map<String, File> files = checkoutFiles(ImmutableList.of(pathInRemoteRepo));
        return Iterables.getOnlyElement(files.values());
    }

    /**
     * Checkout multiple file in one go
     *
     * @param pathesInRemoteRepo
     * @return original path in remote repository to file path in local one
     */
    public Map<String, File> checkoutFiles(Collection<String> pathesInRemoteRepo) {
        try {
            doCheckout(pathesInRemoteRepo);
            return pathesInRemoteRepo.stream().collect(Collectors.toMap(Function.identity(), path -> buildFileInLocalRepo(path)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
