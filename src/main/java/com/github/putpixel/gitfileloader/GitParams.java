package com.github.putpixel.gitfileloader;

import java.io.Serializable;

public class GitParams implements Serializable {

    private static final long serialVersionUID = -4620085740541627832L;

    private String privateKeyPath;
    private String login;
    private String password;

    private String repositoryUrl;
    private String remoteBranch = "refs/remotes/origin/master";

    private String postfix;

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public GitParams privateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
        return this;
    }

    public String getPostfix() {
        return postfix;
    }

    public GitParams postfix(String postfix) {
        this.postfix = postfix;
        return this;
    }

    public String getLogin() {
        return login;
    }

    public GitParams login(String login) {
        this.login = login;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public GitParams password(String password) {
        this.password = password;
        return this;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public GitParams repositoryUrl(String repoPath) {
        this.repositoryUrl = repoPath;
        return this;
    }

    public String getRemoteBranch() {
        return remoteBranch;
    }

    public GitParams remoteBranch(String remoteBranch) {
        this.remoteBranch = remoteBranch;
        return this;
    }

}
