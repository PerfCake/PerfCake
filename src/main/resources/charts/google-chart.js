function drawChart(dataArray, chartDiv, columns, xAxisType, xAxis, yAxis, chartName) {
   var data = new google.visualization.DataTable();
   for (var i = 0; i < dataArray[0].length; i++) {
      if (i == 0 && xAxisType == 1) {
         data.addColumn('datetime', dataArray[0][i]);
      } else if (dataArray[0][i] == "warmUp") {
         data.addColumn('boolean', dataArray[0][i]);
      } else {
         data.addColumn('number', dataArray[0][i]);
      }
   }

   data.addRows(dataArray.slice(1));

   var offset = (new Date()).getTimezoneOffset() * 60 * 1000;

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
         format : xAxisType == 0 ? '#' : (xAxisType == 1 ? 'HH:mm:ss' : '#%'),
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