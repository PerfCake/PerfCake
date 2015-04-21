function drawChart(dataArray, chartDiv, columns, xAxisType, xAxis, yAxis, chartName) {

   function ms2hms(msec_num) {
      var neg = "";
      if (msec_num < 0) {
         msec_num = -msec_num;
         neg = "-";
      }

      var hours        = Math.floor(msec_num / (3600 * 1000));
      msec_num = msec_num % (3600 * 1000);
      var minutes      = Math.floor(msec_num / (60 * 1000));
      msec_num = msec_num % (60 * 1000);
      var seconds      = Math.floor(msec_num / 1000);
      var milliseconds = msec_num % 1000;

      if (hours   < 10) {hours   = "0"+hours;}
      if (minutes < 10) {minutes = "0"+minutes;}
      if (seconds < 10) {seconds = "0"+seconds;}
      if (milliseconds < 10) {
         milliseconds = "00" + milliseconds;
      } else if (milliseconds < 100) {
         milliseconds = "0" + milliseconds;
      }

      return neg + hours + ":" + minutes + ":" + seconds + "." + milliseconds;
   }

   var ho = (new Date(0)).getHours();
   var offset = - (ho >= 12 ? ho - 24 : ho) * 60 * 60 * 1000;
   columns = columns.sort();

   var warmUpCorrection = 0;
   var warmUpPoint = -1;
   for (var i = 2; i < dataArray.length; i++) {
      if (xAxisType == 1) {
         if (dataArray[i][0].getTime() < dataArray[i - 1][0].getTime()) {
            warmUpCorrection = dataArray[i - 1][0].getTime() - offset;
            warmUpPoint = i - 1;
            break;
         }
      } else {
         if (dataArray[i][0] < dataArray[i - 1][0]) {
            warmUpCorrection = dataArray[i - 1][0];
            warmUpPoint = i - 1;
            break;
         }
      }
   }
   if (warmUpPoint >= 0) {
      for (var i = 1; i <= warmUpPoint; i++) {
         if (xAxisType == 1) {
            dataArray[i][0] = new Date(dataArray[i][0].getTime() - warmUpCorrection);
         } else {
            dataArray[i][0] = dataArray[i][0] - warmUpCorrection;
         }
      }
   }

   var data = new google.visualization.DataTable();
   for (var i = 0; i < dataArray[0].length; i++) {
      if (i == 0 && xAxisType == 1) {
         data.addColumn('datetime', dataArray[0][i]);
      } else if (dataArray[0][i] == "warmUp") {
         data.addColumn('boolean', dataArray[0][i]);
      } else {
         data.addColumn('number', dataArray[0][i]);
      }

      if (i == 0) {
         data.addColumn({'type': 'string', 'role': 'tooltip', 'p': {'html': true}});
      }
   }

   for (var i = 1; i < dataArray.length; i++) {
      var legend = "";
      for (var j = 0; j < columns.length; j++) { // place legend after each of the columns

         if (columns[j] == 0 && xAxisType == 1) {
            legend += '<strong>Time: ' + ms2hms(dataArray[i][columns[j]].getTime() - offset);
         } else {
            legend += '<strong>' + dataArray[0][columns[j]] + ': ' + dataArray[i][columns[j]];
         }

         if (columns[j] == 0 && xAxisType == 2) {
            legend = legend + '%';
         }

         legend += '</strong><br />';
      }
      dataArray[i].splice(1, 0, legend);
   }

   columns.splice(1, 0, 1);
   for (var j = 2; j < columns.length; j++) {
      columns[j]++;
   }

   data.addRows(dataArray.slice(1));

   var options = {
      enableInteractivity : true,
      title : chartName,
      height : 500,
      hAxis : {
         title : xAxis,
         minValue : xAxisType == 1 ? new Date(0 + offset) : 0.0,
         gridlines : {
            count : 15
         },
         minorGridlines : {
            count : 11
         },
         format : xAxisType == 0 ? '#' : (xAxisType == 1 ? 'HH:mm:ss SSS' : '#%'),
      },
      vAxis : {
         title : yAxis,
         minValue : 0.0,
         gridlines : {
            count : 13
         },
         minorGridlines : {
            count : 1
         },
         format : "#.#"
      },
      linewidth : 1,
      pointSize : 0,
      interpolateNulls: true,
      allowHtml: true,
      tooltip: { isHtml: true },
      focusTarget: 'category',
   }
   var view = new google.visualization.DataView(data);
   view.setColumns(columns);

   var chart = new google.visualization.ChartWrapper({
      'chartType': 'LineChart',
      'dataTable': view,
      'containerId': chartDiv,
      'options': options,
   });

   chart.draw();
}