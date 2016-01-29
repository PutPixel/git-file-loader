package com.github.putpixel.gitfileloader;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GitLoaderTest {

    private GitLoader gitLoader;

    @Test
    public void cloned_repo_should_contain_only_one_folder_other_should_be_not_checked_out() {
        File localFolder = new File(gitLoader.getLocalPath());
        int filesAndFoldersInside = localFolder.list().length;
        assertEquals("Clonned repo should contain only one folder '.git'", 1, filesAndFoldersInside);
        assertEquals("Clonned repo should contain only one folder '.git'", localFolder.list()[0], ".git");
    }

    @Test
    public void clone_repo_checkout_file_in_folder() {
        String result = gitLoader.checkoutAndGetFileContent("src/test/resources/test.txt");
        assertThat(result, containsString("Test Тест"));
    }

    @After
    public void after() {
        gitLoader.deleteLocalRepository();
    }

    @Before
    public void before() {
        GitParams setup = new GitParams();
        setup.privateKeyPath("./src/test/resources/test.key").password("test1");
        setup.repositoryUrl("git@github.com:PutPixel/git-file-loader.git");
        setup.postfix("for-test");

        gitLoader = new GitLoader(setup);
        gitLoader.cloneRemoteRepository();
    }

}
