$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'sys/role/list',
        datatype: "json",
        colModel: [
            { label: '${lang.sys_string_role_key}', name: 'roleKey', index: "role_key", width: 75, key: true},
            { label: '${lang.sys_string_role_name}', name: 'roleName', index: "role_name", width: 75 },
            { label: '${lang.sys_string_own_department}', name: 'deptName', sortable: false, width: 75 },
            { label: '${lang.sys_string_remark}', name: 'remark', width: 100 },
            { label: '${lang.sys_string_create_time}', name: 'createTime', index: "create_time", width: 80}
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

//菜单树
var menu_ztree;
var menu_setting = {
    data: {
        simpleData: {
            enable: true,
            idKey: "menuKey",
            pIdKey: "parentKey",
            rootPId: ""
        },
        key: {
            url:"nourl"
        }
    },
    check:{
        enable:true,
        nocheckInherit:true
    }
};

//部门结构树
var dept_ztree;
var dept_setting = {
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

//数据树
var data_ztree;
var data_setting = {
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
    },
    check:{
        enable:true,
        nocheckInherit:true,
        chkboxType:{ "Y" : "", "N" : "" }
    }
};

var vm = new Vue({
    el:'#rrapp',
    data:{
        q:{
            roleName: null
        },
        showList: true,
        title:null,
        role:{
            deptKey:null,
            deptName:null
        }
    },
    methods: {
        query: function () {
            vm.reload();
        },
        add: function(){
            vm.showList = false;
            vm.title = "${lang.sys_string_add}";
            vm.role = {deptName:null, deptKey:null};
            vm.getMenuTree(null);

            vm.getDept();

            vm.getDataTree();
        },
        update: function () {
            var roleKey = getSelectedRow();
            if(roleKey == null){
                return ;
            }
            vm.showList = false;
            vm.title = "${lang.sys_string_change}";
            vm.getDataTree();
            vm.getMenuTree(roleKey);
            vm.getDept();
        },
        del: function () {
            var roleKeys = getSelectedRows();
            if(roleKeys == null){
                return ;
            }

            confirm('${lang.sys_string_are_sure_to_delete}？', function(){
                $.ajax({
                    type: "POST",
                    url: baseURL + "sys/role/delete",
                    contentType: "application/json",
                    data: JSON.stringify(roleKeys),
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
        getRole: function(roleKey){
            $.get(baseURL + "sys/role/info/"+roleKey, function(r){
                vm.role = r.role;

                //勾选角色所拥有的菜单
                var menuKeys = vm.role.menuKeys;
                for(var i=0; i < menuKeys.length; i++) {
                    var node = menu_ztree.getNodeByParam("menuKey", menuKeys[i]);
                    menu_ztree.checkNode(node, true, false);
                }

                //勾选角色所拥有的部门数据权限
                var deptKeys = vm.role.deptKeys;
                for(var i=0; i < deptKeys.length; i++) {
                    var node = data_ztree.getNodeByParam("deptKey", deptKeys[i]);
                    data_ztree.checkNode(node, true, false);
                }

                vm.getDept();
            });
        },
        saveOrUpdate: function () {
            //获取选择的菜单
            var nodes = menu_ztree.getCheckedNodes(true);
            var menuKeys = new Array();
            for(var i=0; i < nodes.length; i++) {
                menuKeys.push(nodes[i].menuKey);
            }
            vm.role.menuKeys = menuKeys;

            //获取选择的数据
            var nodes = data_ztree.getCheckedNodes(true);
            var deptKeys = new Array();
            for(var i=0; i < nodes.length; i++) {
                deptKeys.push(nodes[i].deptKey);
            }
            vm.role.deptKeys = deptKeys;

            var url = "sys/role/save";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.role),
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
        getMenuTree: function(roleKey) {
            //加载菜单树
            $.get(baseURL + "sys/menu/list", function(r){
                menu_ztree = $.fn.zTree.init($("#menuTree"), menu_setting, r);
                //展开所有节点
                menu_ztree.expandAll(true);
                if(roleKey != null){
                    vm.getRole(roleKey);
                }
            });
        },
        getDataTree: function(roleId) {
            //加载菜单树
            $.get(baseURL + "sys/dept/list", function(r){
                data_ztree = $.fn.zTree.init($("#dataTree"), data_setting, r);
                //展开所有节点
                data_ztree.expandAll(true);
            });
        },
        getDept: function(){
            //加载部门树
            $.get(baseURL + "sys/dept/list", function(r){
                dept_ztree = $.fn.zTree.init($("#deptTree"), dept_setting, r);
                var node = dept_ztree.getNodeByParam("deptKey", vm.role.deptKey);
                if(node != null){
                    dept_ztree.selectNode(node);
                    vm.role.deptName = node.name;
                }
            })
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
                    var node = dept_ztree.getSelectedNodes();
                    //选择上级部门
                    vm.role.deptKey = node[0].deptKey;
                    vm.role.deptName = node[0].name;
                    layer.close(index);
                }
            });
        },
        reload: function () {
            vm.showList = true;
            var page = $("#jqGrid").jqGrid('getGridParam','page');
            $("#jqGrid").jqGrid('setGridParam',{
                postData:{'roleName': vm.q.roleName},
                page:page
            }).trigger("reloadGrid");
        }
    }
});