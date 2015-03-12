scenario "můj super scénář"
  qsName "test" propA "hello"
  run 10.s with 4.threads
  generator "DefaultMessageGenerator" senderTaskQueueSize 3000 values 1,2,3
  sender "DummySender" target "httpbin.org" headers name:"Gromit",id:1234 anotherProp 12.s
  reporter "WarmUpReporter"
  reporter "ThroughputStatsReporter" coolProp "value"
    destination "CsvDestination" every 3.s path '${perfcake.scenario}-stats.csv' enabled
    destination "ConsoleDestination" every 5.percent disabled
  reporter "ResponseTimeStatsReporter"
    destination "ConsoleDestination" every 10.percent
//  message file:"plain.txt" send 10.times
  message content:"Hello World"
//  message "file://soubor"
  message "Simple text" propA "kukuk" headers name:"Franta",count:10 validate "text1"
  validation fast disabled
    validator "RegExpValidator" id "text1" pattern "I am a fish!"
end