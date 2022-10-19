# timechanger
A Java agent which changes time (such as new Date(), System.currentTimeMillis(), ...) of java process. 

It intercepts System.currentTimeMillis() method calls and applies the offset to the return value.  
So the objects and methods that use System.currentTimeMillis() as their time source (Such as new Date(), Calendar.getInstance() or LocalDateTime.now()) are also changed.

## How to Use
1. Set desired offset millis by using system property `TimeChangerAgent.OFFSETMILLIS`  
(eg. `-DTimeChangerAgent.OFFSETMILLIS=86400000` increases 1 day, `-DTimeChangerAgent.OFFSETMILLIS=-604800000` decreases 1 week)
1. Run your java program with using timechanger as a java agent:  
`-javaagent:/path/to/timechanger-nodep-x.x.x.jar -Xbootclasspath/a:/path/to/timechanger-nodep-x.x.x.jar`

### Example
```
java -javaagent:/path/to/timechanger-nodep-x.x.x.jar -Xbootclasspath/a:/path/to/timechanger-nodep-x.x.x.jar \
-DTimeChangerAgent.OFFSETMILLIS=3600000 \
$JAVA_OPTS \
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
-Xbootclasspath/a:/path/to/timechanger-nodep-x.x.x.jar \
-DTimeChangerAgent.OFFSETMILLIS=3600000 \
$JAVA_OPTS \
your_java_program
```

## Utilities
TimeChangerUtil contains some utility methods:
- setTo(Date)
- setOffsetMillis(long)
- clearOffsetMillis()
- getActualTimeMillis()
- isChangingTime()

[Sample.java](src/main/java/ga/d400x/timechanger/sample/Sample.java) controls time-change using above utility methods.

## Limitations
- Timechanger cannot change essential Java classes loaded by Bootstrap classloader before executing Java agents' premain.
- Need Java1.8 or above

