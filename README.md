# OkDownload

A Reliable, Flexible, Fast and Powerful download engine.

![][okdownload_svg]
[![][build_status_svg]][build_status_link]
[![codecov](https://codecov.io/gh/lingochamp/okdownload/branch/master/graph/badge.svg)](https://codecov.io/gh/lingochamp/okdownload)

---

## WHY REWRITE DOWNLOADER

- FileDownloader framework is not easy to write unit-test, it is not a testable framework, so it is not stable enough.
- The core library of FileDownloader is too complex and not pure enough, so 6K+ star 1K+ fork with around 10 PR.

## Compare to FileDownloader

- Unit test coverage is very high.
- Simpler interface.
- Task priority support.
- Uri file to store output-stream.
- The core library is Pure and light.
- More flexible callback mechanism and listener.
- More flexible to expand each part of OkDownload.
- Fewer threads to do the same thing without drop performance.
- File-IO thread pool is independent of Network-IO thread pool.
- Make sense auto filename from URL if can't find from response header.

## SAMPLE

![][sample_home_img]

![][single_download_img]![][each_block_progress_img]
![][bunch_download_img]![][queue_download_img]

## DEBUG

You can use [okcat](https://github.com/Jacksgong/okcat) to read the detail on the sample(`-c` is just for clear old adb log before running):
![][okcat_img]

```
okcat -y=okcat-okdownload -c
```

## USING

```
// core
com.liulishuo.okdownload:okdownload:{latest_version}
// provide sqlite to store breakpoints
com.liulishuo.okdownload:sqlite:{latest_version}
// provide okhttp to connect to backend
com.liulishuo.okdownload:okhttp:{latest_version}
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository](https://oss.sonatype.org/content/repositories/snapshots/), you can include on your gradle project through:

```groovy
repositories {
    maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
}
```

```groovy
// core
com.liulishuo.okdownload:okdownload:1.0.0-SNAPSHOT
// provide sqlite to store breakpoints
com.liulishuo.okdownload:sqlite:1.0.0-SNAPSHOT
// provide okhttp to connect to backend
com.liulishuo.okdownload:okhttp:1.0.0-SNAPSHOT
```

## SAMPLE

#### 1. Start a task

```java
task = new DownloadTask.Builder(url, parentFile)
         .setFilename(filename)
         // the minimal interval millisecond for callback progress
         .setMinIntervalMillisCallbackProcess(30)
         // do re-download even if the task has already been completed in the past.
         .setPassIfAlreadyCompleted(false)
         .build();

task.enqueue(listener);

// cancel
task.cancel();
```

#### 2. Start tasks

```java
// This method is optimize specially for bunch of tasks
DownloadTask.enqueue(tasks, listener);

// cancel, this method is also optmize specially for bunch of tasks
DownloadTask.cancel(tasks);
```

#### 3. Start queue

```java
DownloadContext.Builder builder = new DownloadContext.QueueSet()
        .setParentPathFile(parentFile)
        .setMinIntervalMillisCallbackProcess(150)
        .commit();
builder.bind(url1);
builder.bind(url2).addTag(key, value);
builder.bind(url3).setTag(tag);
builder.setListener(contextListener);

DownloadTask task = new DownloadTask.Builder(url4, parentFile)
        .setPriority(10).build();
builder.bindSetTask(task);

DownloadContext context = builder.build();

context.startOnParallel(listener);

// stop
context.stop();
```

#### 4. Get download state beyond listener

```java
Status status = StatusUtil.getStatus(task)

status = StatusUtil.getStatus(url, parentPath, null);
status = StatusUtil.getStatus(url, parentPath, filename);

boolean isCompleted = StatusUtil.isCompleted(task);
isCompleted = StatusUtil.isCompleted(url, parentPath, null);
isCompleted = StatusUtil.isCompleted(url, parentPath, filename);

Status completedOrUnknown = StatusUtil.isCompletedOrUnknown(task);

// If you set tag, so just get tag
task.getTag();
task.getTag(xxx);
```

#### 5. Get BreakpointInfo beyond listener

```java
BreakpointInfo info = OkDownload.with().breakpointStore().get(it);

info = StatusUtil.getCurrentInfo(url, parentPath, null);
info = StatusUtil.getCurrentInfo(url, parentPath, filename);
```


## WHY OKDOWNLOAD

### STABILITY and RELIABLE

- [x] Cover unit tests as far as possible
- [x] Precheck whether free space is enough to store target file when try to pre-allocate length of whole resouce with `PreAllocateException` and `EndCase.PRE_ALLOCATE_FAILED`
- [x] Preallocate the length of target file from disk space when start download on very beginning
- [x] Cover the case of there is the same task is running/waiting on OkDownload, another one will be interrupt when required to `enqueue`/`execute`
- [x] Cover the case of there is a task is writing to or will be writing to the same file, another one will be interrupted when `enqueue`/`execute` or get a filename from the response.
- [x] It will never start completed block again automatically when you resume a task
- [x] It ensures the data on block is sync and persist to the physical disk from the file system when the block-thread is finished
- [x] It ensures the length of data on breakpoint-store is less than or equal to length of real data on physical disk to ensure resuming from breakpoint on the next time never damage file or loss data
- [x] It enables resumable for a task only if it passes `ResumeAvailableLocalCheck` and `ResumeAvailableResponseCheck` to ensure breakpoint can't damage the file and the file on backend isn't changed
- [x] Cover the case of file is changed on the backend 
- [x] Cover the case of redirect response on okDownload
- [x] Fix the range automatically when a range of block is wrong
- [x] Check whether the local increase length is equal to the `content-length` on the response to make sure local data is right for each block
- [x] Check the first block and last block especially to cover boundary case
- [x] Ensure cancel operation is effective even if connection is waiting for response, input stream is writing, disconnect is very slow or breakpoint data is synching to the database
- [x] Ensure start operation is effective even if tons of tasks need to be start
- [x] Always saving proceed of each task, so the breakpoint is always resumable even if the process is kill, since your import `com.liulishuo.okdownload:sqlite`
- [x] Using trial connect to check whether resume available and get instance-length to ensure resume correct and avoid unnecessary TCP window cache before downloading each block
- [x] Using the content length hid on the `Content-Range` when there isn't valid `Content-Length` on the response header
- [x] Using the another HEAD method request to check out the certain instance-length when the `Content-Range` and `Transfer-Encoding` isn't exist on the response header for the trial connection.
- [x] Check whether the network is really available before start downloading

### FLEXIBLE and PERFORMANCE

- [x] Support task priority
- [x] Support using `Uri` as target file reference
- [x] Support replace Url on `BreakpointStore` for the case of old Url is discard but its data still resumable
- [x] Combine output-streams of multi-blocks on one handler `MultiPointOutputStream`
- [x] Provide `SpeedCalculator` to calculate instance speed or average speed on `DownloadListener`
- [x] Provide `StatusUtil` to find status of task or breakpoint-info of task anytime, anywhere 
- [x] Provide various callback listeners to meet all requirements you want with `DownloadListener1`,`DownloadListener2`,etc..
- [x] Provide function to find running task reference on `DownloadDispatcher#findSameTask`
- [x] Support one task with several various listeners and manage them easily with `UnifiedListenerManager`
- [x] Support control batch of tasks with `DownloadContext`
- [x] Support only download on Wi-Fi network type
- [x] Support control the whole queue size of running task on `DownloadDispatcher` with `DownloadDispatcher.setMaxParallelRunningCount`
- [x] Provide `DownloadSerialQueue` to control dynamic serial download queue.
- [] Provide `DownloadQueueListener` to listen to the process of batch of tasks
- [x] Support `RemitStoreOnSQLite` to cover the case of many small tasks raise many useless database operation.
- [x] Support split any count of block to download one task
- [x] Design as light as possible to download without drop performance
- [x] Flexible thread pools on OkDownload to let OkDownload lighter
- [x] Split the network I/O thread pool and file system I/O thread pool to avoid two resources block each other
- [x] Support defines callback downloading process on UI-thread asynchronous or on Block-thread synchronized for each task with `DownloadTask.Builder#setAutoCallbackToUIThread`
- [x] Support defines the minimum interval millisecond between two callback of `DownloadListener#fetchProgress` for each task with `DownloadTask.Builder#setMinIntervalMillisCallbackProcess`
- [x] Support defines read buffer size for each task with `DownloadTask.Builder#setReadBufferSize`
- [x] Support define flush buffer size for each task with `DownloadTask.Builder#setFlushBufferSize`
- [x] Support defines sync buffer size for each task with `DownloadTask.Builder#setSyncBufferSize`
- [x] Support downloading task synchronized with `DownloadTask#execute` and asynchronous with `DownloadTask#enqueue`
- [x] Support customize connection handler with implementing your own `DownloadConnection` and valid it with `OkDownload.Builder#connectionFactory`
- [x] Support customizes download strategy with implementing your own `DownloadStrategy` to determine block-count for each task, whether resumable for the eatch task when receiving response, whether need to split to several blocks for eatch task, determine filename of task for eatch task, and valid it with `OkDownload.Builder#downloadStrategy`
- [x] Support customize processing file strategy with implement your own `ProcessFileStrategy` to determine how is OkDownload to handle the processing file, whether resumable when get breakpoint info on very begining, and valid it with `OkDownload.Builder#processFileStrategy`
- [x] Support customize breakpoint store with implement your own `BreakpointStore` to store all resumable breakpoint infos on your store and valid it with `OkDownload.Builder#breakpointStore`
- [x] Support customize callback dispatcher with implment your own `CallbackDispatcher` to handle the event of callback to `DownloadListener` and valid with `Okdownload.Builder#callbackDispatcher`
- [x] Support customize download dispatcher with implement your own `DownloadDispatcher` to control all download task
- [x] Support customize output stream handler with implement your own `DownloadOutputStream` to control output stream for each task
- [x] Support register one whole download monitor to statistic all key step for each task easily with `DownloadMonitor`
- [x] Support custom Logger used on okdownload with `Util.setLogger()` or just want to print log on the console with `Util.enableConsoleLog()`
- [x] Support cancel and start a bunch of tasks instead of single task with special optimize with `DownloadTask.enqueue(tasks, listener)` and `DownloadTask.cancel(tasks)`
- [x] Keep cancel is efficient with let the last i/o operation flying on background and resolve the conflict the next time for same file i/o operation with `FileLock` class jobs. 
- [] Support speed limit
- [] Support download task on independent process with import `okdownload-process`
- [] Support RxJava
- [x] Support using OkHttp as connection handler with import `com.liulishuo.okdownload:okhttp`
- [] Support using OkDownload on kotlin style such as DSL with import `okdownload-kotlin-enhance`
- [] Provide Benchmark on `benchmark` application
- [] Provide `connectionPool` library to maintain connection for default `DownloadUrlConnection` instead of disconnect each time

## GRAPH

![][listener_img]
![][check_before_chain_img]

## LICENSE

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
