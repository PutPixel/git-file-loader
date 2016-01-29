package com.github.putpixel.gitfileloader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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
import org.eclipse.jgit.util.FS;

import com.google.common.base.Preconditions;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class GitLoader {

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteLocalRepository() {
        repo.close();
        new File(localPath).delete();
    }

    public String getLocalPath() {
        return localPath;
    }

    public void cloneRemoteRepository() {
        try {
            Git.cloneRepository()
                    .setURI(setup.getRepositoryUrl())
                    .setCredentialsProvider(createCredentials())
                    .setNoCheckout(true)
                    .setDirectory(new File(localPath)).call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    private UsernamePasswordCredentialsProvider createCredentials() {
        if (setup.getPrivateKeyPath() != null) {
            SshSessionFactory.setInstance(new JschConfigSessionFactory() {
                @Override
                protected void configure(OpenSshConfig.Host host, Session session) {
                    // This can be removed, but the overriden method is required since JschConfigSessionFactory is abstract
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

    public String checkoutAndGetFileContent(String filePathInRepo) {
        try {
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
                        Preconditions.checkState(getUpdated().containsKey(filePathInRepo), "No path found in repo: " + filePathInRepo);
                        String value = getUpdated().get(filePathInRepo);
                        getUpdated().clear();
                        getUpdated().put(filePathInRepo, value);
                    }
                };
                dco.setFailOnConflict(true);
                dco.checkout();
            } finally {
                dc.unlock();
            }

            return com.google.common.io.Files.toString(new File(localPath + "/" + filePathInRepo), Charset.defaultCharset());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
