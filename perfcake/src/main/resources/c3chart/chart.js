var chart_${baseName} = c3.generate({
   bindto: '#chart_${baseName}_div',
   size: {
      height: ${height}
   },
   data: {
      x: '${xAxis}',
      rows: ${baseName},
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
   }
});
