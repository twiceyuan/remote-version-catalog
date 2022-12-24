# Remote Version Catalog

[![](https://jitpack.io/v/twiceyuan/remote-version-catalog.svg)](https://jitpack.io/#twiceyuan/remote-version-catalog)

一个加载远程 toml 配置文件的 Gradle 自用插件，使用方式如下：

在 `gradle.properties` 文件添加以下属性：

| 字段名                           | 约束  | 含义                           |
|-------------------------------|-----|------------------------------|
| remote.version.catalog.url    | 必选  | toml 文件的下载地址                 |
| remote.version.catalog.name   | 必选  | 配置名称，会根据该配置创建 VersionCatalog |
| remote.version.catalog.expire | 可选  | 缓存过期时间，单位毫秒，默认值为一天（86400000） |
| remote.version.catalog.path   | 可选  | toml 文件存储路径，默认存储 .gradle 路径下 |

例如：

```
# Remote Config 配置文件地址，可以自己写个存在 Gist 上，并使用文末的链接2 获取固定链接
remote.version.catalog.url=https://gist.githubusercontent.com/twiceyuan/941c695ba0297c56878bc8ca2806b931/raw
# VersionCatalog 的引用名称
remote.version.catalog.name=common
# VersionCatalog 文件的存储路径。默认在 .gradle 一般不会跟随 VCS，设置 . 为项目根目录
remote.version.catalog.path=.
```

添加插件引用 `settings.gradle`：

```
rootProject.name = "remote-version-catalog-demo"
include ':app'

buildscript {
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }

    dependencies {
        classpath 'io.github.twiceyuan:remote-version-catalog:1.1'
    }
}

apply plugin: 'remote-version-catalog'
```

在依赖中引用：

```
implementation common.kotlin.stdlib
```

toml 配置文件可以使用 Gist 服务，参考文末链接。

## Thanks

1. 相关 API 使用参考了团队大佬 [@5peak2me](https://github.com/5peak2me) 的插件实现
2. [如何获取一个 gist 文件的永久链接](https://gist.github.com/atenni/5604615)

## License

MIT
