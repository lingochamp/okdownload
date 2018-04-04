# OkDownload

可靠，灵活，高性能以及强大的下载引擎。

![][okdownload_svg]
[![][build_status_svg]][build_status_link]
[![codecov](https://codecov.io/gh/lingochamp/okdownload/branch/master/graph/badge.svg)](https://codecov.io/gh/lingochamp/okdownload)

> [ENGLISH](https://github.com/lingochamp/okdownload)

---

## I. 为什么选择

实际上，OkDownload是FileDownloader2，它继承了所有FileDownloader的优点，甚至做了更多的优化以及更多的特性，相关更详细的描述请移步到[这里](https://github.com/lingochamp/okdownload/wiki/Why-Choose-OkDownload)

## II. 如何引入

我们将OkDownload发布到了`jcenter`、`mavenCentral`以及`Sonatype's snapshots`仓库，更多关于如何引入OkDownload请移步到[这里](https://github.com/lingochamp/okdownload/wiki)

## III. 如何使用

- 简单的使用场景如启动、取消、队列下载或者是获取任务的状态与信息，请移步到[这里](https://github.com/lingochamp/okdownload/wiki/Simple-Use-Guideline)
- 高级的使用场景如设置最大并行运行的任务数目、设置延时提交数据库的延时毫秒亦或是如何注入自定义组件，请移步到[这里](https://github.com/lingochamp/okdownload/wiki/Advanced-Use-Guideline)

## IV. 案例项目

### 调试

> [如何调试](https://github.com/lingochamp/okdownload/wiki/Debug-OkDownload)

![][okcat_img]

### 截图

![][sample_home_img]

![][single_download_img]![][each_block_progress_img]
![][bunch_download_img]![][queue_download_img]

## V. LICENSE

```
Copyright (c) 2017 LingoChamp Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[okdownload_svg]: https://img.shields.io/badge/Android-Okdownload-green.svg
[sample_home_img]: https://github.com/lingochamp/okdownload/raw/master/art/sample-home.jpeg
[single_download_img]: https://github.com/lingochamp/okdownload/raw/master/art/single-download.gif
[each_block_progress_img]: https://github.com/lingochamp/okdownload/raw/master/art/each-block-progress.gif
[bunch_download_img]: https://github.com/lingochamp/okdownload/raw/master/art/bunch-download.gif
[queue_download_img]: https://github.com/lingochamp/okdownload/raw/master/art/queue-download.gif
[listener_img]: https://github.com/lingochamp/okdownload/raw/master/art/listener.png
[check_before_chain_img]: https://github.com/lingochamp/okdownload/raw/master/art/check_before_chain.png
[build_status_svg]: https://travis-ci.org/lingochamp/okdownload.svg?branch=master
[build_status_link]: https://travis-ci.org/lingochamp/okdownload
[okcat_img]: https://github.com/lingochamp/okdownload/raw/master/art/okcat.png
