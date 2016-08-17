var chart_${baseName} = c3.generate({
   bindto: '#chart_${baseName}_div',
   size: {
      height: ${height}
   },
   data: {
      x: '${xAxisKey}',
      rows: ${baseName},
   },
   color: {
      pattern: ['#dd4814', '#e58200', '#edc003', '#cc617f', '#7a74bc', '#1f17a1', '#6bd5e2', '#afdd56', '#448d1a']
   },
   axis: {
      x: {
         tick: {
            format: ${format}
         },
         label: {
            text: '${xAxis}',
            position: 'outer-center'
         }
      },
      y: {
         label: {
            text: '${yAxis}',
            position: 'outer-middle'
         }
      }
   },
   grid: {
      x: {
         show: true
      },
      y: {
         show: true
      }
   },
   line: {
      connectNull: true
   },
   padding: {
      right: 40
   }
});
