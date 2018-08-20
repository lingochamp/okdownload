### 1.0.4

_2018-08-08_

#### Feature

- Provide the filedownloader interface wrapper for okdownload.

#### Bug Fix

- Fix unexpected response header field `content-length` raise `NumberFormatException`. closes #107
- Fix first download for a resource and kill the process within 1.5s after task is started, then launch process again the wrong complete status raise issue. closes #82
- Fix recieve `task-busy` status with `taskEnd` callback when invoke enqueue task instantly on the `taskEnd` callback synchronize.

### 1.0.3

_2018-05-16_

#### Feature

- Discard the breakpoint when the response instance-length is changed. refs #31
- Cover the case of resource content-length is changed between block-connection and trial-connection for single-block task. refs #31
- Add default user-agent when there isn't any user-agent provide
- Cover the special case of backend response 416 on trial-connect because of range:0-0 but values is valid on response header. refs #55

#### Bug Fix

- Release connection for all cases of download-chain finish
- Fix progress callbacks is missing for DownloadListener1, DownloadListener4, DownloadListener4WithSpeed when you re-attach one of them to UnifiedListenerManager manually. refs #52

### 1.0.2

_2018-04-28_

#### Feature

- Change the default sync buffer interval millisecond from 3000 to 2000, because the default value also 2000ms on download-provider.
- Carry the user set header fields on trial-connection also. refs #42
- Support set connection count through `DownloadTask.Builder#setConnectionCount`. refs #31
- Support set pre-allocate-length through `DownloadTask.Builder#setPreAllocateLength` refs #31

#### Stability

- Using the more stability design to ensure there is only one thread operates with output-stream and ensure close output-stream after the last operation for one task's output-stream. refs #39 

#### Bug Fix

- End the store-info operation after output-stream real finished instead of on download-dispatcher for each task. refs #39
- Fix exception isn't the real one when creating a task with wrong params. refs #41
- Fix the remit optimize isn't effective on remit-database when the task is canceled within remit-delay-time.
- Fix the illegal runtime exception is raised when download chunked resources. refs #35
- Fix the result of `getTotalOffset` and `getTotalLength` isn't right on `BreakpointInfo` when chunked resource completed download. refs #35
- Fix can't resume when the file length is larger than current response instant-length because of the old file isn't delete when the file is dirty.

### 1.0.1

_2018-04-20_

#### Feature

- Support get info reference from task reference because after task completed we delete info from the cache for health lifecycle but people may need info reference for the task
- Add `taskDownloadFromBreakpoint` and `taskDownloadFromBeginning` for `DownloadMonitor`
- Use exactly range even for the last block to cover the case of some resource response unexpected content range when request range is to end. closes #17
- Support `taskEnd` on `DownloadContextListener` which will carry back how many counts remain after this callback
- Support cancel task just used its id. closes #30

#### Stability

- Cover the case of the length of the local file is larger than the total length of info for the `BreakpointLocalCheck`

#### Bug Fix

- Fix unexpected completed returned by `StatusUtil` when the file exists but the user doesn't use persist database such as `sqlite`
- Fix DownloadTask.toBuilder set a duplicate filename in some cases
- Fix raise runtime type exception when removing data from the breakpoint store straightly after canceling the task. closes #34
- Fix `fd` isn't released manually when download finished which may raise OOM when there are a large number of tasks that are continuously initiated.

### 1.0.0

_2018-04-06_

- First blood!
