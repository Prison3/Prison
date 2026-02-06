# Prison Core System Documentation

## Repository Information

- **GitHub**: [https://github.com/Prison3/prison](https://github.com/Prison3/prison)
- **License**: Apache License 2.0
[![Badge](https://img.shields.io/badge/link-996.icu-%23FF4D5B.svg?style=flat-square)](https://996.icu/#/en_US)
[![LICENSE](https://img.shields.io/badge/license-Anti%20996-blue.svg?style=flat-square)](https://github.com/996icu/996.ICU/blob/master/LICENSE)
[![Slack](https://img.shields.io/badge/slack-996icu-green.svg?style=flat-square)](https://join.slack.com/t/996icu/shared_invite/enQtNjI0MjEzMTUxNDI0LTkyMGViNmJiZjYwOWVlNzQ3NmQ4NTQyMDRiZTNmOWFkMzYxZWNmZGI0NDA4MWIwOGVhOThhMzc3NGQyMDBhZDc)
[![HitCount](http://hits.dwyl.com/996icu/996ICU.svg)](http://hits.dwyl.com/996icu/996ICU)

> ⚠️ **996.ICU Statement**: This project supports the 996.ICU movement. We believe in reasonable working hours and work-life balance.

## Purpose and Use Cases

Prison is a core framework for Android app virtualization/sandboxing. It isolates and runs target apps inside a host
process, and provides component-level proxies, process/permission/storage redirection, and system service interception
so apps can run and be managed in a controlled environment.

**Typical Uses**:
- Run multiple isolated app instances inside a single host (multi-user isolation / multi-instance)
- Component-level virtualization (Activity/Service/Receiver/Provider) and proxy replacement
- Compatibility hardening and anti-detection (system services, file system, class loading, property checks)
- Runtime environment redirection (data dirs, library paths, config and permission isolation)

**Applicable Scenarios**:
- Enterprise app isolation and data sandboxing
- Multi-instance execution and behavior validation in test/regression environments
- Fine-grained interception and auditing of system service calls

## Documentation Index

### 1. [ActivityStack Internals](./docs/ActivityStack工作原理.en.md)
Explains how ActivityStack manages task stacks, handles launch modes, and implements Activity replacement.

**Main Topics**:
- Activity launch flow
- Activity replacement mechanism
- Launch mode handling (singleTop, singleTask, singleInstance)
- Intent flags handling
- Lifecycle management
- Task stack synchronization

### 2. [ActivityManagerService Internals](./docs/ActivityManagerService工作原理.en.md)
Explains how ActivityManagerService manages components in a multi-user environment.

**Main Topics**:
- Multi-user isolation
- Activity management
- Service management
- Broadcast management
- ContentProvider management
- Process management integration

### 3. [InjectorManager Proxy Class Reference](./docs/InjectorManager代理类功能说明.en.md)
Describes all proxy classes in InjectorManager and their roles.

**Main Topics**:
- Detailed overview of 85+ proxy classes
- Function-based organization
- Categories of core, functional, and compatibility proxies
- Proxy workflow and considerations

### 4. [Virtualization of Android's Four Core Components](./docs/Android四大组件虚拟化工作原理.en.md)
Introduces the virtualization principles for Activity, Service, BroadcastReceiver, and ContentProvider.

**Main Topics**:
- Core design of component virtualization
- Activity virtualization details
- Service virtualization details
- BroadcastReceiver virtualization details
- ContentProvider virtualization details
- Proxy component management and multi-user isolation
- Unified workflow and key technical points

### 5. [Launching Unregistered Activities in Prison](./docs/Prison框架启动未注册Activity原理.en.md)
Explains how Prison launches Activities not statically registered in AndroidManifest.xml.

**Main Topics**:
- Proxy replacement mechanism
- Message loop interception
- Save/restore flow
- HCallbackProxy internals
- Full launch flow diagram

### 6. [HCallbackProxy Internals and Technical Details](./docs/HCallbackProxy工作原理与技术细节.en.md)

### 7. [App Detection vs. Prison and Anti-Detection Mechanisms](./docs/应用检测Prison框架方案与反检测机制.en.md)
Explains how apps detect Prison and how Prison counters those detections.

**Main Topics**:
- File system detection and countermeasures
- Package/process detection and countermeasures
- Class loader detection and countermeasures
- Stack trace detection and countermeasures
- System property detection and countermeasures
- System service detection and countermeasures
- Summary and hardening suggestions

### 8. [Native Hook Internals and Technical Details](./docs/NativeHook工作原理与技术细节.en.md)
Introduces the principles and implementation of native hooks in Prison.

**Main Topics**:
- JNI hook mechanism (ArtMethod structure modifications)
- Native function hook mechanism (DobbyHook)
- Hook class implementations and roles
- Hook initialization flow
- Multi-version compatibility handling
- Key technical points and caveats

## Quick Navigation

### Activity Management Flow
1. **Start request** → `PActivityManager.startActivity()`
2. **Service call** → `ActivityManagerService.startActivity()`
3. **Stack management** → `ActivityStack.startActivityLocked()`
4. **Proxy replacement** → `getStartStubActivityIntentInner()`
5. **Process start** → `ProcessManagerService.startProcessLocked()`
6. **Activity restore** → `ProxyActivity.onCreate()`

### Key Classes

#### ActivityStack
- **Path**: `core/src/main/java/com/android/prison/system/am/ActivityStack.java`
- **Role**: Manage Activity stacks and tasks
- **Key methods**: `startActivityLocked()`, `getStartStubActivityIntentInner()`

#### ActivityManagerService
- **Path**: `core/src/main/java/com/android/prison/system/am/ActivityManagerService.java`
- **Role**: System service, manages multi-user components
- **Key methods**: `startActivity()`, `startService()`, `sendBroadcast()`

#### ProxyActivity
- **Path**: `core/src/main/java/com/android/prison/proxy/ProxyActivity.java`
- **Role**: Proxy Activity to replace original Activities
- **Key methods**: `onCreate()`

#### ProxyActivityRecord
- **Path**: `core/src/main/java/com/android/prison/proxy/ProxyActivityRecord.java`
- **Role**: Persist original Activity info
- **Key methods**: `saveStub()`, `create()`

## Architecture Overview

```
PActivityManager (client)
    ↓ AIDL
ActivityManagerService (system service)
    ↓
UserSpace (user space)
    ├─ ActivityStack (Activity stack)
    │   └─ TaskRecord[] (task stacks)
    │       └─ ActivityRecord[] (Activity records)
    │
    ├─ ActiveServices (Service management)
    │   └─ RunningServiceRecord[]
    │
    └─ mIntentSenderRecords (IntentSender records)
        └─ PendingIntentRecord[]
```

## Core Concepts

### 1. User Isolation
Each user has an independent `UserSpace`, including:
- Separate Activity stacks
- Separate Service management
- Separate IntentSender records

### 2. Activity Replacement
Virtualization is implemented in two steps:
1. **Replace ComponentName**: original Activity → ProxyActivity
2. **Restore original Activity**: ProxyActivity.onCreate() → original Activity

### 3. Proxy Mechanism
- **ProxyActivity**: Proxy Activities (P0-P49)
- **ProxyService**: Proxy Service
- **ProxyManifest**: Proxy component name generator

### 4. Process Management
- **ProcessManagerService**: Process lifecycle management
- **ProcessRecord**: Process record holding ActivityThread reference

## Related Files

### Core Classes
- `ActivityStack.java` - Activity stack management
- `ActivityManagerService.java` - System service
- `UserSpace.java` - User space
- `TaskRecord.java` - Task record
- `ActivityRecord.java` - Activity record

### Proxy Classes
- `ProxyActivity.java` - Proxy Activity
- `ProxyActivityRecord.java` - Activity proxy record
- `ProxyManifest.java` - Proxy component name generator
- `ProxyService.java` - Proxy Service
- `ProxyServiceRecord.java` - Service proxy record
- `ProxyBroadcastReceiver.java` - Proxy BroadcastReceiver
- `ProxyBroadcastRecord.java` - BroadcastReceiver proxy record
- `ProxyContentProvider.java` - Proxy ContentProvider

### Manager Classes
- `PActivityManager.java` - Activity manager (client)
- `ProcessManagerService.java` - Process manager
- `PackageManagerService.java` - Package manager

## Flow Diagrams

### Activity Start Flow
```
Client call
    ↓
PActivityManager.startActivity()
    ↓
ActivityManagerService.startActivity()
    ↓
Get UserSpace
    ↓
ActivityStack.startActivityLocked()
    ↓
Parse Intent → find/create TaskRecord
    ↓
Handle launch mode → create ActivityRecord
    ↓
Start process → ProcessManagerService
    ↓
Generate proxy Intent → replace ComponentName
    ↓
Save original info → ProxyActivityRecord.saveStub()
    ↓
Start ProxyActivity
    ↓
ProxyActivity.onCreate() → restore original Activity
    ↓
onActivityCreated() → add to task stack
```

### Service Start Flow
```
Client call
    ↓
PActivityManager.startService()
    ↓
ActivityManagerService.startService()
    ↓
Get UserSpace
    ↓
ActiveServices.startService()
    ↓
Parse Service → start process
    ↓
Create RunningServiceRecord
    ↓
Generate proxy Intent → ProxyService
    ↓
Start ProxyService
```

## Notes

### Thread Safety
- All operations on UserSpace must be synchronized
- ActivityStack and ActiveServices operations must be locked

### Process Management
- Process start failures throw RuntimeException
- Ensure ProcessManagerService is running

### Proxy Activity Selection
- Select proxy class by process ID (vpid)
- Each process uses a different proxy class (P0-P49)
- Avoid proxy class conflicts

### Task Stack Synchronization
- Periodically call `synchronizeTasks()` to sync system task stacks
- Remove tasks that no longer exist in the system

## Further Reading

- [Android Activity launch modes](https://developer.android.com/guide/components/activities/tasks-and-back-stack)
- [Android multi-user support](https://source.android.com/devices/tech/admin/multi-user)
- [AIDL interface definition](https://developer.android.com/guide/components/aidl)

## Changelog

- 2024-02-02: Initial documentation
  - ActivityStack internals
  - ActivityManagerService internals
  - InjectorManager proxy class reference
  - Virtualization of Android's four core components

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) file for details.

## Links

- **GitHub Repository**: [https://github.com/Prison3/prison](https://github.com/Prison3/prison)
- **996.ICU Movement**: [https://996.icu](https://996.icu) | [GitHub](https://github.com/996icu/996.ICU)

---
# Prison Core System Documentation

## Repository Information

- **GitHub**: [https://github.com/Prison3/prison](https://github.com/Prison3/prison)
- **License**: Apache License 2.0
- **996.ICU**: [https://996.icu](https://996.icu) | [https://github.com/996icu/996.ICU](https://github.com/996icu/996.ICU)

> ⚠️ **996.ICU Statement**: This project supports the 996.ICU movement. We believe in reasonable working hours and work-life balance.

## Purpose and Use Cases

Prison is a core framework for Android app virtualization/sandboxing. It isolates and runs target apps inside a host
process, providing component-level proxies, process/permission/storage redirection, and system service interception,
so target apps can run and be managed in a controlled environment.

**Typical uses**:
- Run multiple independent app instances inside a single host (multi-user isolation / multi-instance)
- Component-level virtualization (Activity/Service/Receiver/Provider) and proxy replacement
- Compatibility hardening and anti-detection (system services, file system, class loading, property checks)
- Runtime environment redirection (data directories, library paths, configuration, and permission isolation)

**Applicable scenarios**:
- Enterprise app isolation and data sandboxing
- Multi-instance testing and behavior validation in QA/regression
- Scenarios requiring fine-grained interception and auditing of system service calls

## Document Index

### 1. [ActivityStack Internals](./docs/ActivityStack工作原理.en.md)
Explains how ActivityStack manages task stacks, launch modes, and Activity replacement.

**Highlights**:
- Activity launch flow
- Activity replacement mechanism
- Launch mode handling (singleTop, singleTask, singleInstance)
- Intent flags handling
- Lifecycle management
- Task stack synchronization

### 2. [ActivityManagerService Internals](./docs/ActivityManagerService工作原理.en.md)
Explains how ActivityManagerService manages components in a multi-user environment.

**Highlights**:
- Multi-user isolation
- Activity management
- Service management
- Broadcast management
- ContentProvider management
- Process management integration

### 3. [InjectorManager Proxy Reference](./docs/InjectorManager代理类功能说明.en.md)
Describes the function and role of all proxy classes in InjectorManager.

**Highlights**:
- Detailed descriptions of 85+ proxy classes
- Organization by functional categories
- Core proxies, feature proxies, and compatibility proxies
- Proxy workflow and usage notes

### 4. [Virtualization of Android Components](./docs/Android四大组件虚拟化工作原理.en.md)
Comprehensive overview of how Activity, Service, BroadcastReceiver, and ContentProvider are virtualized.

**Highlights**:
- Core design principles
- Activity virtualization in depth
- Service virtualization in depth
- BroadcastReceiver virtualization in depth
- ContentProvider virtualization in depth
- Proxy component management and multi-user isolation
- Unified workflows and key technical points

### 5. [Launching Unregistered Activities in Prison](./docs/Prison框架启动未注册Activity原理.en.md)
Explains how Prison launches Activities not statically registered in AndroidManifest.xml.

**Highlights**:
- Proxy replacement mechanism
- Message loop interception
- Information storage and restoration
- HCallbackProxy internals
- End-to-end flow diagram

### 6. [HCallbackProxy Internals and Details](./docs/HCallbackProxy工作原理与技术细节.en.md)

### 7. [App Detection and Anti-Detection Strategies](./docs/应用检测Prison框架方案与反检测机制.en.md)
Explains common detection methods used against Prison and the framework's countermeasures.

**Highlights**:
- File system detection and countermeasures
- Package/process detection and countermeasures
- Class loader detection and countermeasures
- Stack trace detection and countermeasures
- System property detection and countermeasures
- System service detection and countermeasures
- Summary and hardening suggestions

### 8. [Native Hook Internals and Details](./docs/NativeHook工作原理与技术细节.en.md)
Explains the design and implementation of native hooks in Prison.

**Highlights**:
- JNI hook mechanism (ArtMethod structure patching)
- Native function hooking (DobbyHook)
- Implementation and roles of hook classes
- Hook initialization flow
- Multi-version compatibility
- Key technical notes

## Quick Navigation

### Activity Management Flow
1. **Launch request** → `PActivityManager.startActivity()`
2. **Service call** → `ActivityManagerService.startActivity()`
3. **Stack management** → `ActivityStack.startActivityLocked()`
4. **Proxy replacement** → `getStartStubActivityIntentInner()`
5. **Process launch** → `ProcessManagerService.startProcessLocked()`
6. **Activity restoration** → `ProxyActivity.onCreate()`

### Key Classes

#### ActivityStack
- **Location**: `core/src/main/java/com/android/prison/system/am/ActivityStack.java`
- **Role**: Manages Activity stacks and tasks
- **Key methods**: `startActivityLocked()`, `getStartStubActivityIntentInner()`

#### ActivityManagerService
- **Location**: `core/src/main/java/com/android/prison/system/am/ActivityManagerService.java`
- **Role**: System service that manages multi-user components
- **Key methods**: `startActivity()`, `startService()`, `sendBroadcast()`

#### ProxyActivity
- **Location**: `core/src/main/java/com/android/prison/proxy/ProxyActivity.java`
- **Role**: Proxy Activity that performs Activity replacement
- **Key methods**: `onCreate()`

#### ProxyActivityRecord
- **Location**: `core/src/main/java/com/android/prison/proxy/ProxyActivityRecord.java`
- **Role**: Stores original Activity information
- **Key methods**: `saveStub()`, `create()`

## Architecture Overview

```
PActivityManager (client)
    ↓ AIDL
ActivityManagerService (system service)
    ↓
UserSpace (user space)
    ├─ ActivityStack (Activity stack)
    │   └─ TaskRecord[] (task stack)
    │       └─ ActivityRecord[] (activity records)
    │
    ├─ ActiveServices (Service management)
    │   └─ RunningServiceRecord[]
    │
    └─ mIntentSenderRecords (IntentSender records)
        └─ PendingIntentRecord[]
```

## Core Concepts

### 1. User Isolation
Each user has an independent `UserSpace` that includes:
- Independent Activity stack
- Independent Service management
- Independent IntentSender records

### 2. Activity Replacement
Virtualization is achieved in two steps:
1. **Replace ComponentName**: original Activity → ProxyActivity
2. **Restore original Activity**: `ProxyActivity.onCreate()` → original Activity

### 3. Proxy Mechanism
- **ProxyActivity**: Activity proxy (P0-P49)
- **ProxyService**: Service proxy
- **ProxyManifest**: Proxy component name generator

### 4. Process Management
- **ProcessManagerService**: Manages process lifecycle
- **ProcessRecord**: Process record holding ActivityThread reference

## Related Files

### Core Classes
- `ActivityStack.java` - Activity stack management
- `ActivityManagerService.java` - System service
- `UserSpace.java` - User space
- `TaskRecord.java` - Task record
- `ActivityRecord.java` - Activity record

### Proxy Classes
- `ProxyActivity.java` - Activity proxy
- `ProxyActivityRecord.java` - Activity proxy record
- `ProxyManifest.java` - Proxy component name generator
- `ProxyService.java` - Service proxy
- `ProxyServiceRecord.java` - Service proxy record
- `ProxyBroadcastReceiver.java` - BroadcastReceiver proxy
- `ProxyBroadcastRecord.java` - BroadcastReceiver proxy record
- `ProxyContentProvider.java` - ContentProvider proxy

### Manager Classes
- `PActivityManager.java` - Activity manager (client)
- `ProcessManagerService.java` - Process manager
- `PackageManagerService.java` - Package manager

## Workflow Diagrams

### Activity Launch Flow
```
Client call
    ↓
PActivityManager.startActivity()
    ↓
ActivityManagerService.startActivity()
    ↓
Acquire UserSpace
    ↓
ActivityStack.startActivityLocked()
    ↓
Parse Intent → find/create TaskRecord
    ↓
Handle launch mode → create ActivityRecord
    ↓
Start process → ProcessManagerService
    ↓
Generate proxy Intent → replace ComponentName
    ↓
Save original info → ProxyActivityRecord.saveStub()
    ↓
Launch ProxyActivity
    ↓
ProxyActivity.onCreate() → restore original Activity
    ↓
onActivityCreated() → attach to task stack
```

### Service Launch Flow
```
Client call
    ↓
PActivityManager.startService()
    ↓
ActivityManagerService.startService()
    ↓
Acquire UserSpace
    ↓
ActiveServices.startService()
    ↓
Resolve Service → start process
    ↓
Create RunningServiceRecord
    ↓
Generate proxy Intent → ProxyService
    ↓
Start ProxyService
```

## Notes

### Thread Safety
- All operations on UserSpace must be synchronized
- ActivityStack and ActiveServices must be locked

### Process Management
- Process start failures throw RuntimeException
- Ensure ProcessManagerService is healthy

### Proxy Activity Selection
- Select proxy class based on process ID (vpid)
- Each process uses a distinct proxy class (P0-P49)
- Avoid proxy class conflicts

### Task Stack Synchronization
- Periodically call `synchronizeTasks()` to sync system tasks
- Remove tasks that no longer exist in the system

## Further Reading

- [Android Activity launch modes](https://developer.android.com/guide/components/activities/tasks-and-back-stack)
- [Android multi-user support](https://source.android.com/devices/tech/admin/multi-user)
- [AIDL interface definition](https://developer.android.com/guide/components/aidl)

## Changelog

- 2024-02-02: Initial documentation
  - ActivityStack internals
  - ActivityManagerService internals
  - InjectorManager proxy reference
  - Android components virtualization

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) file for details.

## Links

- **GitHub Repository**: [https://github.com/Prison3/prison](https://github.com/Prison3/prison)
- **996.ICU Movement**: [https://996.icu](https://996.icu) | [GitHub](https://github.com/996icu/996.ICU)

---
