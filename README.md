# timechanger
A Java agent which changes date/time (such as new Date(), System.currentTimeMillis(), ...) of java process.  
Any change or recompile to your source codes is ***NOT*** needed.

This agent transforms classes to intercept System.currentTimeMillis() method calls and applies the offset to the return value.  
So the classes that use System.currentTimeMillis() as their time source (like java.util.Date, java.util.Calendar or java.time. APIs) are also changed.

## How to Use
Add following command options to your java command:

1. Desired offset millis by using system property `TimeChangerAgent.OFFSETMILLIS`  
(eg. `-DTimeChangerAgent.OFFSETMILLIS=86400000` increases 1 day,  
 `-DTimeChangerAgent.OFFSETMILLIS=-604800000` decreases 1 week)
1. Set timechanger as a java agent:  
`-javaagent:/path/to/timechanger-nodep-x.x.x.jar`

### Example
```
java -javaagent:/path/to/timechanger-nodep-x.x.x.jar \
-DTimeChangerAgent.OFFSETMILLIS=3600000 \
other_java_opts \
your_java_program
```

## Other Options
- Verbose output (to STDERR)  
`-DTIMECHANGER_DEBUG`
- Exclude from time change  
`exclude=path,path_starts_with*,...`  
***NOTE:*** exclude option only excludes intercepting `System.currentTimeMillis()` call in matched classes. Other time related methods (like new Date()) return changed time.

### Example
```
java -DTIMECHANGER_DEBUG \
-javaagent:/path/to/timechanger-nodep-x.x.x.jar=exclude=org.apache.logging.log4j.core.appender.rolling.* \
-DTimeChangerAgent.OFFSETMILLIS=3600000 \
other_java_opts \
your_java_program
```

## Utilities
[TimeChangerUtil](src/main/java/ga/d400x/timechanger/util/TimeChangerUtil.java) contains some utility methods:
- setTo(Date)
- setOffsetMillis(long)
- clearOffsetMillis()
- getActualTimeMillis()
- isChangingTime()

[Sample.java](src/main/java/ga/d400x/timechanger/sample/Sample.java) controls time-change using above utility methods.

## Limitations
- Timechanger cannot change essential Java classes loaded by Bootstrap classloader before executing Java agents' premain.
- Need Java1.8 or above
- With gradle 7+, build will fail(https://github.com/shevek/jarjar/issues/22). Gradle 6.9.2 worked fine.
