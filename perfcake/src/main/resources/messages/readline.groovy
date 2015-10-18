System.in.eachLine() {line ->
   if (line.equals("exit")) {
      System.exit()
   } else {
      println "you entered: $line"
   }
}