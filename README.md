# OkDownload

A flexible and powerful downloader engine.


---

## Why rewrite downloader

- FileDownloader framework is not easy to write unit-test, it is not testable framework, so it is not stable enough.
- The core library of FileDownloader is too complex and not pure enough, so 5K+ star 1K+ fork with around 10 PR.

## Compare to FileDownloader

- Unit test coverage is very high.
- Simpler interface.
- Task priority support.
- Uri file to store output-stream.
- The core library is Pure and light.
- More flexible callback mechanism and listener.
- More flexible to expand each part of OkDownload.
- Fewer threads to do same thing without drop performance.
- File-IO thread pool is independent of Network-IO thread pool.
- Make sense auto filename from url if can't find from response header.

## LICENSE

```
Copyright (C) 2017 Jacksgong(jacksgong.com)

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
