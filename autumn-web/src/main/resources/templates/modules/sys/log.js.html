$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'sys/log/list',
        datatype: "json",
        colModel: [			
			{ label: 'id', name: 'id', width: 30, key: true },
			{ label: '${lang.sys_string_username}', name: 'username', width: 50 },
			{ label: '${lang.sys_string_query_username}', name: 'operation', width: 70 },
			{ label: '${lang.sys_string_request_method}', name: 'method', width: 150 },
			{ label: '${lang.sys_string_request_parameter}', name: 'params', width: 80 },
            { label: '${lang.sys_string_execute_duration}', name: 'time', width: 80 },
			{ label: '${lang.sys_string_ip_address}', name: 'ip', width: 70 },
			{ label: '${lang.sys_string_create_time}', name: 'createDate', width: 90 }
        ],
		viewrecords: true,
        height: 385,
        rowNum: 10,
		rowList : [10,30,50],
        rownumbers: true, 
        rownumWidth: 25, 
        autowidth:true,
        multiselect: false,
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
			key: null
		},
	},
	methods: {
		query: function () {
			vm.reload();
		},
		reload: function (event) {
			var page = $("#jqGrid").jqGrid('getGridParam','page');
			$("#jqGrid").jqGrid('setGridParam',{ 
				postData:{'key': vm.q.key},
                page:page
            }).trigger("reloadGrid");
		}
	}
});