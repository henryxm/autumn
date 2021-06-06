$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'sys/user/list',
        datatype: "json",
        colModel: [
            { label: '${lang.sys_string_uuid}', name: 'uuid', index: "uuid", width: 75, key: true },
			{ label: '${lang.sys_string_username}', name: 'username', width: 75 },
            { label: '${lang.sys_string_own_department}', name: 'deptName', sortable: false, width: 75 },
			{ label: '${lang.sys_string_email}', name: 'email', width: 90 },
			{ label: '${lang.sys_string_phone_number}', name: 'mobile', width: 100 },
			{ label: '${lang.sys_string_status}', name: 'status', width: 60, formatter: function(value, options, row){
				return value === 0 ? 
					'<span class="label label-danger">${lang.sys_string_forbidden}</span>' :
					'<span class="label label-success">${lang.sys_string_normal}</span>';
			}},
			{ label: '${lang.sys_string_create_time}', name: 'createTime', index: "create_time", width: 85}
        ],
		viewrecords: true,
        height: 385,
        rowNum: 10,
		rowList : [10,30,50],
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
var setting = {
    data: {
        simpleData: {
            enable: true,
            idKey: "deptKey",
            pIdKey: "parentKey",
            rootPId: ""
        },
        key: {
            url:"nourl"
        }
    }
};
var ztree;

var vm = new Vue({
    el:'#rrapp',
    data:{
        q:{
            username: null
        },
        showList: true,
        title:null,
        roleList:{},
        user:{
            status:1,
            deptKey:null,
            deptName:null,
            roleKeys:[]
        }
    },
    methods: {
        query: function () {
            vm.reload();
        },
        add: function(){
            vm.showList = false;
            vm.title = "${lang.sys_string_add}";
            vm.roleList = {};
            vm.user = {deptName:null, deptKey:null, status:1, roleKeys:[]};

            //获取角色信息
            this.getRoleList();

            vm.getDept();
        },
        getDept: function(){
            //加载部门树
            $.get(baseURL + "sys/dept/list", function(r){
                ztree = $.fn.zTree.init($("#deptTree"), setting, r);
                var node = ztree.getNodeByParam("deptKey", vm.user.deptKey);
                if(node != null){
                    ztree.selectNode(node);

                    vm.user.deptName = node.name;
                }
            })
        },
        update: function () {
            var uuid = getSelectedRow();
            if(uuid == null){
                return ;
            }

            vm.showList = false;
            vm.title = "${lang.sys_string_change}";

            vm.getUser(uuid);
            //获取角色信息
            this.getRoleList();
        },
        del: function () {
            var uuids = getSelectedRows();
            if(uuids == null){
                return ;
            }

            confirm('${lang.sys_string_are_sure_to_delete}？', function(){
                $.ajax({
                    type: "POST",
                    url: baseURL + "sys/user/delete",
                    contentType: "application/json",
                    data: JSON.stringify(uuids),
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
        saveOrUpdate: function () {
            var url = vm.user.uuid == null ? "sys/user/save" : "sys/user/update";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.user),
                success: function(r){
                    if(r.code === 0){
                        alert('${lang.sys_string_success}', function(){
                            vm.reload();
                        });
                    }else{
                        alert(r.msg);
                    }
                }
            });
        },
        getUser: function(uuid){
            $.get(baseURL + "sys/user/info/"+uuid, function(r){
                vm.user = r.user;
                vm.user.password = null;

                vm.getDept();
            });
        },
        getRoleList: function(){
            $.get(baseURL + "sys/role/select", function(r){
                vm.roleList = r.list;
            });
        },
        deptTree: function(){
            layer.open({
                type: 1,
                offset: '50px',
                skin: 'layui-layer-molv',
                title: "${lang.sys_string_select_department}",
                area: ['300px', '450px'],
                shade: 0,
                shadeClose: false,
                content: jQuery("#deptLayer"),
                btn: ['${lang.sys_string_confirm}', '${lang.sys_string_cancel}'],
                btn1: function (index) {
                    var node = ztree.getSelectedNodes();
                    //选择上级部门
                    vm.user.deptKey = node[0].deptKey;
                    vm.user.deptName = node[0].name;

                    layer.close(index);
                }
            });
        },
        reload: function () {
            vm.showList = true;
            var page = $("#jqGrid").jqGrid('getGridParam','page');
            $("#jqGrid").jqGrid('setGridParam',{
                postData:{'username': vm.q.username},
                page:page
            }).trigger("reloadGrid");
        }
    }
});