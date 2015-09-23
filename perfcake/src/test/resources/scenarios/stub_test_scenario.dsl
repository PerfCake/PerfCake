scenario "můj super scénář"
  qsName "test" propA "hello"
  run 10.s with 4.threads
  generator "DefaultMessageGenerator" senderTaskQueueSize 3000
  sender "TestSender" target "httpbin.org" delay 12.s
  reporter "WarmUpReporter"
  reporter "ThroughputStatsReporter" minimumEnabled false
    destination "CsvDestination" every 3.s path '${perfcake.scenario}-stats.csv' enabled
    destination "ConsoleDestination" every 5.percent disabled
  reporter "ResponseTimeStatsReporter"
    destination "ConsoleDestination" every 10.percent
  message file:"message1.xml" send 10.times
  message content:"Hello World" values 1,2,3
  message "file://message2.txt" validate "text1","text2"
  message "Simple text" propA "kukuk" headers name:"Franta",count:10 validate "text1, text2"
  validation fast disabled
    validator "RegExpValidator" id "text1" pattern "I am a fish!"
    validator "RegExpValidator" id "text2" pattern "I was a fish!"
end