package com.github.putpixel.gitfileloader;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

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

	@Test
	public void clone_repo_checkout_two_file_in_folder() {
		String result = gitLoader.checkoutAndGetFileContent("src/test/resources/test.txt");
		assertThat(result, containsString("Test Тест"));

		result = gitLoader.checkoutAndGetFileContent("src/test/resources/test2.txt");
		assertThat(result, containsString("Test2 Тест2"));
	}

	@Test
	public void clone_repo_checkout_collection_of_files() throws IOException {
		String file1 = "src/test/resources/test.txt";
		String file2 = "src/test/resources/test2.txt";
		ImmutableList<String> pathes = ImmutableList.of(file1, file2);

		Map<String, File> result = gitLoader.checkoutFiles(pathes);

		assertThat(result.entrySet(), Matchers.hasSize(2));
		Assert.assertTrue(result.get(file1).exists());
		Assert.assertTrue(result.get(file2).exists());

		String file1Content = Files.toString(result.get(file1), Charset.defaultCharset());
		assertThat(file1Content, containsString("Test Тест"));

		String file2Content = Files.toString(result.get(file2), Charset.defaultCharset());
		assertThat(file2Content, containsString("Test2 Тест2"));
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
