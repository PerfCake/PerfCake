int mid = Integer.valueOf(message.headers["mid"]);
File outFile = new File(message.headers["tmpPath"], message.payload + "-" + mid);
println "Touching " + outFile.absolutePath;
outFile << mid;
