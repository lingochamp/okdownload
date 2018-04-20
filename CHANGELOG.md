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
