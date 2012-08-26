var connector = remote.connect("alfresco");
var data = connector.get("/uberblog/list");

if(data == undefined || data == null){
	model.posts = null;
}else{
	results = eval('(' + data + ')');
	model.posts = results["posts"];
}
