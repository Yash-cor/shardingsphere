+++
title = "ElasticJob UI 发布指南"
weight = 4
chapter = true
+++

## GPG 设置

详情请参见[发布指南](/cn/involved/release/shardingsphere/)。

## 发布 Apache Maven 中央仓库

**1. 设置 settings.xml 文件**

将以下模板添加到 `~/.m2/settings.xml` 中，所有密码需要加密后再填入。
加密设置可参考[这里](http://maven.apache.org/guides/mini/guide-encryption.html)。

```xml
<settings>
  <servers>
    <server>
      <id>apache.snapshots.https</id>
      <username> <!-- APACHE LDAP 用户名 --> </username>
      <password> <!-- APACHE LDAP 加密后的密码 --> </password>
    </server>
    <server>
      <id>apache.releases.https</id>
      <username> <!-- APACHE LDAP 用户名 --> </username>
      <password> <!-- APACHE LDAP 加密后的密码 --> </password>
    </server>
  </servers>
</settings>
```

**2. 更新版本说明**

```
https://github.com/apache/shardingsphere-elasticjob-ui/blob/master/RELEASE-NOTES.md
```

**3. 创建发布分支**

假设从 Github 下载的 ElasticJob-UI 源代码在 `~/elasticjob-ui/` 目录；假设即将发布的版本为 `${RELEASE.VERSION}`。
创建 `${RELEASE.VERSION}-release` 分支，接下来的操作都在该分支进行。

```shell
## ${name} 为源码所在分支，如：master，dev-4.x
git clone --branch ${name} https://github.com/apache/shardingsphere-elasticjob-ui.git ~/elasticjob-ui
cd ~/elasticjob-ui/
git pull
git checkout -b ${RELEASE.VERSION}-release
git push origin ${RELEASE.VERSION}-release
```

**4. 发布预校验**

```shell
cd ~/elasticjob-ui
mvn release:prepare -Prelease -Darguments="-DskipTests" -DautoVersionSubmodules=true -DdryRun=true -Dusername=${Github 用户名}
```

-Prelease：选择 release 的 profile，这个 profile 会打包所有源码、jar 文件以及 ElasticJob-UI 的可执行二进制包。

-DautoVersionSubmodules=true：作用是发布过程中版本号只需要输入一次，不必为每个子模块都输入一次。

-DdryRun=true：演练，即不产生版本号提交，不生成新的 tag。

**5. 准备发布**

首先清理发布预校验本地信息。

```shell
cd ~/elasticjob-ui
mvn release:clean
```

然后准备执行发布。

```shell
cd ~/elasticjob-ui
mvn release:prepare -Prelease -Darguments="-DskipTests" -DautoVersionSubmodules=true -DpushChanges=false -Dusername=${Github用户名}
```

和上一步演练的命令基本相同，去掉了 -DdryRun=true 参数。

-DpushChanges=false：不要将修改后的版本号和 tag 自动提交至 Github。

将本地文件检查无误后，提交至 Github。

```shell
git push origin ${RELEASE.VERSION}-release
git push origin --tags
```

**6. 部署发布**

```shell
cd ~/elasticjob-ui
mvn release:perform -Prelease -Darguments="-DskipTests" -DautoVersionSubmodules=true -Dusername=${Github用户名}
```

## 发布 Apache SVN 仓库

**1. 检出 ShardingSphere 发布目录**

如无本地工作目录，则先创建本地工作目录。

```shell
mkdir -p ~/ss_svn/dev/
cd ~/ss_svn/dev/
```

创建完毕后，从 Apache SVN 检出 ShardingSphere 发布目录。

```shell
svn --username=${APACHE LDAP 用户名} co https://dist.apache.org/repos/dist/dev/shardingsphere
cd ~/ss_svn/dev/shardingsphere
```

**2. 添加gpg公钥**

仅第一次部署的账号需要添加，只要 `KEYS` 中包含已经部署过的账户的公钥即可。

```shell
gpg -a --export ${GPG用户名} >> KEYS
```

**3. 将待发布的内容添加至 SVN 目录**

创建版本号目录。

```shell
mkdir -p ~/ss_svn/dev/shardingsphere/elasticjob-ui-${RELEASE.VERSION}
cd ~/ss_svn/dev/shardingsphere/elasticjob-ui-${RELEASE.VERSION}
```

将源码包和二进制包添加至 SVN 工作目录。

```shell
cp -f ~/elasticjob-ui/shardingsphere-elasticjob-ui-distribution/shardingsphere-elasticjob-ui-src-distribution/target/*.zip ~/ss_svn/dev/shardingsphere/elasticjob-ui-${RELEASE.VERSION}
cp -f ~/elasticjob-ui/shardingsphere-elasticjob-ui-distribution/shardingsphere-elasticjob-ui-src-distribution/target/*.zip.asc ~/ss_svn/dev/shardingsphere/elasticjob-ui-${RELEASE.VERSION}
cp -f ~/elasticjob-ui/shardingsphere-elasticjob-ui-distribution/shardingsphere-elasticjob-lite-ui-bin-distribution/target/*.tar.gz ~/ss_svn/dev/shardingsphere/elasticjob-ui-${RELEASE.VERSION}
cp -f ~/elasticjob-ui/shardingsphere-elasticjob-ui-distribution/shardingsphere-elasticjob-lite-ui-bin-distribution/target/*.tar.gz.asc ~/ss_svn/dev/shardingsphere/elasticjob-ui-${RELEASE.VERSION}
cp -f ~/elasticjob-ui/shardingsphere-elasticjob-ui-distribution/shardingsphere-elasticjob-cloud-ui-bin-distribution/target/*.tar.gz ~/ss_svn/dev/shardingsphere/elasticjob-ui-${RELEASE.VERSION}
cp -f ~/elasticjob-ui/shardingsphere-elasticjob-ui-distribution/shardingsphere-elasticjob-cloud-ui-bin-distribution/target/*.tar.gz.asc ~/ss_svn/dev/shardingsphere/elasticjob-ui-${RELEASE.VERSION}
```

**4. 生成文件签名**

```shell
shasum -a 512 apache-shardingsphere-elasticjob-${RELEASE.VERSION}-ui-src.zip > apache-shardingsphere-elasticjob-${RELEASE.VERSION}-ui-src.zip.sha512
shasum -b -a 512 apache-shardingsphere-elasticjob-${RELEASE.VERSION}-lite-ui-bin.tar.gz > apache-shardingsphere-elasticjob-${RELEASE.VERSION}-lite-ui-bin.tar.gz.sha512
shasum -b -a 512 apache-shardingsphere-elasticjob-${RELEASE.VERSION}-cloud-ui-bin.tar.gz > apache-shardingsphere-elasticjob-${RELEASE.VERSION}-cloud-ui-bin.tar.gz.sha512
```

**5. 提交 Apache SVN**

```shell
cd ~/ss_svn/dev/shardingsphere/
svn add elasticjob-ui-${RELEASE.VERSION}
svn --username=${APACHE LDAP 用户名} commit -m "release elasticjob-ui-${RELEASE.VERSION}"
```
## 检查发布结果

**检查 sha512 哈希**

```shell
shasum -c apache-shardingsphere-elasticjob-${RELEASE.VERSION}-ui-src.zip.sha512
shasum -c apache-shardingsphere-elasticjob-${RELEASE.VERSION}-lite-ui-bin.tar.gz.sha512
shasum -c apache-shardingsphere-elasticjob-${RELEASE.VERSION}-cloud-ui-bin.tar.gz.sha512
```

**检查 gpg 签名**

首先导入发布人公钥。从 svn 仓库导入 KEYS 到本地环境。（发布版本的人不需要再导入，帮助做验证的人需要导入，用户名填发版人的即可）。

```shell
curl https://dist.apache.org/repos/dist/dev/shardingsphere/KEYS >> KEYS
gpg --import KEYS
gpg --edit-key "${发布人的gpg用户名}"
  > trust

Please decide how far you trust this user to correctly verify other users' keys
(by looking at passports, checking fingerprints from different sources, etc.)

  1 = I don't know or won't say
  2 = I do NOT trust
  3 = I trust marginally
  4 = I trust fully
  5 = I trust ultimately
  m = back to the main menu

Your decision? 5

  > save
```

然后进行 gpg 签名检查。

```shell
gpg --verify apache-shardingsphere-elasticjob-${RELEASE.VERSION}-ui-src.zip.asc apache-shardingsphere-elasticjob-${RELEASE.VERSION}-ui-src.zip
gpg --verify apache-shardingsphere-elasticjob-${RELEASE.VERSION}-lite-ui-bin.tar.gz.asc apache-shardingsphere-elasticjob-${RELEASE.VERSION}-lite-ui-bin.tar.gz
gpg --verify apache-shardingsphere-elasticjob-${RELEASE.VERSION}-cloud-ui-bin.tar.gz.asc apache-shardingsphere-elasticjob-${RELEASE.VERSION}-cloud-ui-bin.tar.gz
```
**检查发布文件内容**

**对比源码包与 Github 上 tag 的内容差异**

```
curl -Lo tag-${RELEASE.VERSION}.zip https://github.com/apache/shardingsphere-elasticjob-ui/archive/${RELEASE.VERSION}.zip
unzip tag-${RELEASE.VERSION}.zip
unzip apache-shardingsphere-elasticjob-${RELEASE.VERSION}-ui-src.zip
diff -r apache-shardingsphere-elasticjob-${RELEASE.VERSION}-ui-src shardingsphere-elasticjob-ui-${RELEASE.VERSION}
```

**检查源码包的文件内容**

- 检查源码包是否包含由于包含不必要文件，致使 tarball 过于庞大；
- 存在 `LICENSE` 和 `NOTICE` 文件；
- `NOTICE` 文件中的年份正确；
- 只存在文本文件，不存在二进制文件；
- 所有文件的开头都有 ASF 许可证；
- 能够正确编译，单元测试可以通过（./mvnw -T 1C install）；
- 检查是否有多余文件或文件夹，例如空文件夹等。

**检查二进制包的文件内容**

解压缩 `apache-shardingsphere-elasticjob-${RELEASE.VERSION}-lite-ui-bin.tar.gz` 和 `apache-shardingsphere-elasticjob-${RELEASE.VERSION}-cloud-ui-bin.tar.gz`
进行如下检查:

- 存在 `LICENSE` 和 `NOTICE` 文件；
- `NOTICE` 文件中的年份正确；
- 所有文本文件开头都有 ASF 许可证；
- 检查第三方依赖许可证：
  - 第三方依赖的许可证兼容；
  - 所有第三方依赖的许可证都在 `LICENSE` 文件中声明；
  - 依赖许可证的完整版全部在 `license` 目录；
  - 如果依赖的是 Apache 许可证并且存在 `NOTICE` 文件，那么这些 `NOTICE` 文件也需要加入到版本的 `NOTICE` 文件中。

## 发起投票

**投票阶段**

1. ShardingSphere 社区投票，发起投票邮件到 `dev@shardingsphere.apache.org`。PMC 需要先按照文档检查版本的正确性，然后再进行投票。
经过至少 72 小时并统计到3个 `+1 PMC member` 票后，即可进入下一阶段的投票。

2. 宣布投票结果，发起投票结果邮件到 `dev@shardingsphere.apache.org`。

**投票模板**

1. ShardingSphere 社区投票模板

标题：

```
[VOTE] Release Apache ShardingSphere ElasticJob UI ${RELEASE.VERSION}
```

正文：

```
Hello ShardingSphere Community,

This is a call for vote to release Apache ShardingSphere ElasticJob UI version ${RELEASE.VERSION}

Release notes:
https://github.com/apache/shardingsphere-elasticjob-ui/blob/master/RELEASE-NOTES.md

The release candidates:
https://dist.apache.org/repos/dist/dev/shardingsphere/elasticjob-ui-${RELEASE.VERSION}/

Git tag for the release:
https://github.com/apache/shardingsphere-elasticjob-ui/tree/${RELEASE.VERSION}/

Release Commit ID:
https://github.com/apache/shardingsphere-elasticjob-ui/commit/xxxxxxxxxxxxxxxxxxxxxxx

Keys to verify the Release Candidate:
https://dist.apache.org/repos/dist/dev/shardingsphere/KEYS

Look at here for how to verify this release candidate:
https://shardingsphere.apache.org/community/en/involved/release/elasticjob-ui/

GPG user ID:
${YOUR.GPG.USER.ID}

The vote will be open for at least 72 hours or until necessary number of votes are reached.

Please vote accordingly:

[ ] +1 approve

[ ] +0 no opinion

[ ] -1 disapprove with the reason

PMC vote is "+1 binding", all others is "+1 non-binding".

Checklist for reference:

[ ] Download links are valid.

[ ] Checksums and PGP signatures are valid.

[ ] Source code distributions have correct names matching the current release.

[ ] LICENSE and NOTICE files are correct for each ShardingSphere repo.

[ ] All files have license headers if necessary.

[ ] No compiled archives bundled in source archive.
```

2. 宣布投票结果模板：

标题：

```
[RESULT][VOTE] Release Apache ShardingSphere ElasticJob UI ${RELEASE.VERSION}
```

正文：

```
We’ve received 3 "+1 binding" votes and one "+1 non-binding" vote:

+1 binding, xxx
+1 binding, xxx
+1 binding, xxx

+1 non-binding, xxx

Thank you everyone for taking the time to review the release and help us.
I will process to publish the release and send ANNOUNCE.
```

## 完成发布

**1. 将源码、二进制包以及 KEYS 从 svn 的 dev 目录移动到 release 目录**

```shell
svn mv https://dist.apache.org/repos/dist/dev/shardingsphere/elasticjob-ui-${RELEASE.VERSION} https://dist.apache.org/repos/dist/release/shardingsphere/ -m "transfer packages for elasticjob-ui-${RELEASE.VERSION}"
svn delete https://dist.apache.org/repos/dist/release/shardingsphere/KEYS -m "delete KEYS"
svn cp https://dist.apache.org/repos/dist/dev/shardingsphere/KEYS https://dist.apache.org/repos/dist/release/shardingsphere/ -m "transfer KEYS for elasticjob-ui-${RELEASE.VERSION}"
```

**2. 合并 Github 的 release 分支到 `master`, 合并完成后删除 release 分支**

```shell
git checkout master
git merge origin/${RELEASE.VERSION}-release
git pull
git push origin master
git push --delete origin ${RELEASE.VERSION}-release
git branch -d ${RELEASE.VERSION}-release
```

**3. 发布 Docker (cloud-ui)**

3.1 准备工作

本地安装 Docker，并启动服务。

3.2 编译 Docker 镜像

```shell
git checkout ${RELEASE.VERSION}
cd ~/elasticjob-ui/shardingsphere-elasticjob-ui-distribution/shardingsphere-elasticjob-cloud-ui-bin-distribution/
mvn clean package -Prelease,docker
```

3.3 给本地 Docker 镜像打标记

通过 `docker images` 查看到 IMAGE ID，例如为：e9ea51023687。

```shell
docker tag e9ea51023687 apache/shardingsphere-elasticjob-cloud-ui:latest
docker tag e9ea51023687 apache/shardingsphere-elasticjob-cloud-ui:${RELEASE.VERSION}
```

3.4 发布 Docker 镜像

```shell
docker push apache/shardingsphere-elasticjob-cloud-ui:latest
docker push apache/shardingsphere-elasticjob-cloud-ui:${RELEASE_VERSION}
```

3.5 确认发布成功

登录 [Docker Hub](https://hub.docker.com/r/apache/shardingsphere-elasticjob-cloud-ui/) 查看是否有发布的镜像。

**4. 发布 Docker (lite-ui)**

4.1 准备工作

本地安装 Docker，并启动服务。

4.2 编译 Docker 镜像

```shell
cd ~/elasticjob-ui/shardingsphere-elasticjob-ui-distribution/shardingsphere-elasticjob-lite-ui-bin-distribution/
mvn clean package -Prelease,docker
```

4.3 给本地 Docker 镜像打标记

通过 `docker images` 查看到 IMAGE ID，例如为：e9ea51023687。

```shell
docker tag e9ea51023687 apache/shardingsphere-elasticjob-lite-ui:latest
docker tag e9ea51023687 apache/shardingsphere-elasticjob-lite-ui:${RELEASE.VERSION}
```

4.4 发布 Docker 镜像

```shell
docker login
docker push apache/shardingsphere-elasticjob-lite-ui:latest
docker push apache/shardingsphere-elasticjob-lite-ui:${RELEASE_VERSION}
```

4.5 确认发布成功

登录 [Docker Hub](https://hub.docker.com/r/apache/shardingsphere-elasticjob-lite-ui/) 查看是否有发布的镜像。

**5. GitHub 版本发布**

在 [GitHub Releases](https://github.com/apache/shardingsphere-elasticjob-ui/releases) 页面的 `shardingsphere-elasticjob-ui-${RELEASE_VERSION}` 版本上点击 `Edit`。

编辑版本号及版本说明，并点击 `Publish release`。

**6. 更新下载页面**

等待并确认新的发布版本同步至 Apache 镜像后，更新如下页面：

https://shardingsphere.apache.org/elasticjob/current/en/downloads/

https://shardingsphere.apache.org/elasticjob/current/cn/downloads/

GPG 签名文件和哈希校验文件的下载连接应该使用这个前缀：`https://downloads.apache.org/shardingsphere/`。

`最新版本` 中保留一个最新的版本。

**7. 邮件通知版本发布完成**

发送邮件到 `dev@shardingsphere.apache.org` 和 `announce@apache.org` 通知完成版本发布。

通知邮件模板：

标题：

```
[ANNOUNCE] Apache ShardingSphere ElasticJob UI ${RELEASE.VERSION} available
```

正文：

```
Hi all,

Apache ShardingSphere Team is glad to announce the new release of Apache ShardingSphere ElasticJob UI ${RELEASE.VERSION}.

ElasticJob is a distributed scheduling solution consisting of two separate projects, ElasticJob-Lite and ElasticJob-Cloud.
Through the functions of flexible scheduling, resource management and job management, it creates a distributed scheduling solution suitable for Internet scenarios, and provides diversified job ecosystem through open architecture design. It uses a unified job API for each project. Developers only need code one time and can deploy at will.
ElasticJob became an Apache ShardingSphere Sub project on May 28 2020.

Download Links: https://shardingsphere.apache.org/elasticjob/current/en/downloads/

Release Notes: https://github.com/apache/shardingsphere-elasticjob-ui/blob/master/RELEASE-NOTES.md

Website: http://shardingsphere.apache.org/elasticjob/

ShardingSphere-ElasticJob Resources:
- Issue: https://github.com/apache/shardingsphere-elasticjob-ui/issues/
- Mailing list: dev@shardingsphere.apache.org
- Documents: https://shardingsphere.apache.org/elasticjob/current/en/overview/



- Apache ShardingSphere Team

```
