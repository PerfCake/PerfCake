function ms2hms(msec_num) {
   var neg = "";
   if (msec_num < 0) {
      msec_num = -msec_num;
      neg = "-";
   }

   var hours = Math.floor(msec_num / (3600 * 1000));
   msec_num = msec_num % (3600 * 1000);
   var minutes = Math.floor(msec_num / (60 * 1000));
   msec_num = msec_num % (60 * 1000);
   var seconds = Math.floor(msec_num / 1000);
   var milliseconds = msec_num % 1000;

   if (hours < 10) {
      hours = "0" + hours;
   }
   if (minutes < 10) {
      minutes = "0" + minutes;
   }
   if (seconds < 10) {
      seconds = "0" + seconds;
   }
   if (milliseconds < 10) {
      milliseconds = "00" + milliseconds;
   } else if (milliseconds < 100) {
      milliseconds = "0" + milliseconds;
   }

   return neg + hours + ":" + minutes + ":" + seconds + "." + milliseconds;
}
