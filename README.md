# OkDownload

A Reliable, Flexible, Fast and Powerful download engine.

![][okdownload_svg]
[![][build_status_svg]][build_status_link]
[![codecov](https://codecov.io/gh/lingochamp/okdownload/branch/master/graph/badge.svg)](https://codecov.io/gh/lingochamp/okdownload)
[ ![Download](https://api.bintray.com/packages/jacksgong/maven/OkDownload/images/download.svg) ](https://bintray.com/jacksgong/maven/OkDownload/_latestVersion)
[![][okdownload_snapshot_svg]](https://oss.sonatype.org/content/repositories/snapshots/com/liulishuo/okdownload/)

> [中文文档](https://github.com/lingochamp/okdownload/blob/master/README-zh.md)

---

> P.S. If you ask me, which version is the most stability, I will tell you it's not the version of 1.0.0 or 2.0.0, the most stability version must be the latest version because it is developed with github-flow, not production-flow. So please follow the latest release version and show me your PR. Here is [the changelog for each version](https://github.com/lingochamp/okdownload/blob/master/CHANGELOG.md), it may help you.

## I. WHY CHOOSE

In fact OkDownload is FileDownloader2, which extends all benefits from FileDownloader and beyond. More detail please move to [here](https://github.com/lingochamp/okdownload/wiki/Why-Choose-OkDownload)

## II. HOW TO IMPORT

We publish okdownload on [jcenter](http://jcenter.bintray.com/), [mavenCentral](https://oss.sonatype.org/content/repositories/releases/) and [Sonatype's snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/), more detail about import OkDownload please move to [here](https://github.com/lingochamp/okdownload/wiki)

## III. HOW TO USE

- The simple use case such as start and cancel, download queue or get state or task info, please more to [here](https://github.com/lingochamp/okdownload/wiki/Simple-Use-Guideline)
- The advanced use case such as set max parallel running count, set remit database delay milliseconds or injection components, please move to [here](https://github.com/lingochamp/okdownload/wiki/Advanced-Use-Guideline)

## IV. SAMPLE

### Debug

> [How to Debug](https://github.com/lingochamp/okdownload/wiki/Debug-OkDownload)

![][okcat_img]

### Screenshot

<img src="https://github.com/lingochamp/okdownload/raw/master/art/sample-home.jpeg" width="480">

![][single_download_img]![][each_block_progress_img]
![][bunch_download_img]![][queue_download_img]
![][content_uri_img]![][notification_img]

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

[okdownload_svg]: https://img.shields.io/badge/Android-OkDownload-green.svg
[okdownload_snapshot_svg]: https://img.shields.io/badge/SnapShot-1.0.8-yellow.svg
[sample_home_img]: https://github.com/lingochamp/okdownload/raw/master/art/sample-home.jpeg
[single_download_img]: https://github.com/lingochamp/okdownload/raw/master/art/single-download.gif
[each_block_progress_img]: https://github.com/lingochamp/okdownload/raw/master/art/each-block-progress.gif
[bunch_download_img]: https://github.com/lingochamp/okdownload/raw/master/art/bunch-download.gif
[queue_download_img]: https://github.com/lingochamp/okdownload/raw/master/art/queue-download.gif
[content_uri_img]: https://github.com/lingochamp/okdownload/raw/master/art/content-uri.gif
[notification_img]: https://github.com/lingochamp/okdownload/raw/master/art/notification.gif
[listener_img]: https://github.com/lingochamp/okdownload/raw/master/art/listener.png
[check_before_chain_img]: https://github.com/lingochamp/okdownload/raw/master/art/check_before_chain.png
[build_status_svg]: https://travis-ci.org/lingochamp/okdownload.svg?branch=master
[build_status_link]: https://travis-ci.org/lingochamp/okdownload
[okcat_img]: https://github.com/lingochamp/okdownload/raw/master/art/okcat.png
