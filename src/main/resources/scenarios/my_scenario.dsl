scenario "můj super scénář"
  qsName "test" propA "hello"
  run 10.s with 4.threads
  generator "DefaultMessageGenerator" threadQueueSize 3000
end