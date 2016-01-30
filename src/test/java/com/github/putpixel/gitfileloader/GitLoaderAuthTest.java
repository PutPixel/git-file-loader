package com.github.putpixel.gitfileloader;

import org.junit.After;
import org.junit.Test;

public class GitLoaderAuthTest {

	private GitLoader gitLoader;

	@Test
	public void check_no_pass_phrase() {
		GitParams setup = new GitParams();
		setup.privateKeyPath("./src/test/resources/no_pass.key");
		setup.repositoryUrl("git@github.com:PutPixel/git-file-loader.git");
		setup.postfix("for-test");

		gitLoader = new GitLoader(setup);
		gitLoader.cloneRemoteRepository();
	}

	@Test
	public void check_with_pass_phrase() {
		GitParams setup = new GitParams();
		setup.privateKeyPath("./src/test/resources/test.key").password("test1");
		setup.repositoryUrl("git@github.com:PutPixel/git-file-loader.git");
		setup.postfix("for-test");

		gitLoader = new GitLoader(setup);
		gitLoader.cloneRemoteRepository();
	}

	@After
	public void after() {
		gitLoader.deleteLocalRepository();
	}

}
