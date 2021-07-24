$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL +'gen/generator/list',
        datatype: "json",
        colModel: [			
			{ label: '${lang.sys_string_table_name}', name: 'tableName', width: 100, key: true },
			{ label: '${lang.sys_string_engine}', name: 'engine', width: 70},
			{ label: '${lang.sys_string_table_comment}', name: 'tableComment', width: 100 },
			{ label: '${lang.sys_string_create_time}', name: 'createTime', width: 100 }
        ],
		viewrecords: true,
        height: "auto",
        rowNum: 100,
		rowList : [10,30,50,100,200],
        rownumbers: true, 
        rownumWidth: 25, 
        autowidth:true,
        multiselect: true,
        pager: "#jqGridPager",
        jsonReader : {
            root: "page.list",
            page: "page.currPage",
            total: "page.totalPage",
            records: "page.totalCount"
        },
        prmNames : {
            page:"page", 
            rows:"limit", 
            order: "order"
        },
        gridComplete:function(){
        	//隐藏grid底部滚动条
        	$("#jqGrid").closest(".ui-jqgrid-bdiv").css({ "overflow-x" : "hidden" }); 
        }
    });
});

var vm = new Vue({
	el:'#rrapp',
	data:{
		q:{
			tableName: null,
            genType:null,
            moduleText:"${lang.sys_string_select_generator_solution}",
            genId:null,
            genTypeList:[],
		}
	},
	methods: {
		query: function () {
			$("#jqGrid").jqGrid('setGridParam',{ 
                postData:{'tableName': vm.q.tableName},
                page:1 
            }).trigger("reloadGrid");
		},
		generator: function() {
		    if(null == vm.q.genId){
		        alert("${lang.sys_string_select_generator_solution}");
		        return;
            }

			var tableNames = getSelectedRows();
			if(tableNames == null){
				return ;
			}
            location.href = baseURL + "gen/generator/code?tables=" + tableNames.join()+"&genId="+vm.q.genId;
		},
        resetTable: function() {
            var tableNames = getSelectedRows();
            if(tableNames == null){
                tableNames = [] ;
            }
            confirm('${lang.sys_string_are_sure_to_reset_table}？', function(){
                $.ajax({
                    type: "POST",
                    url: baseURL + "gen/generator/reset",
                    contentType: "application/json",
                    data: JSON.stringify(tableNames),
                    success: function(r){
                        if(r.code == 0){
                            alert('${lang.sys_string_success}', function(){
                                vm.reload();
                            });
                        }else{
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        selectType: function(a) {
           vm.q.genType = vm.q.genTypeList[a];
           vm.q.genId = vm.q.genType.id;
           vm.q.moduleText = vm.q.genType.moduleText;
        }
	},
    mounted: function () {
        this.$nextTick(function () {
            $.get(baseURL + "gen/gentype/list",function (data) {
                vm.q.genTypeList = data.page.list;
            })
        });
    }
});

