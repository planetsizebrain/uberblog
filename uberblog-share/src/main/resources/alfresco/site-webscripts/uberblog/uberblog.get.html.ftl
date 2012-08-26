<script type="text/javascript">//<![CDATA[
	new Alfresco.widget.DashletResizer("${args.htmlid}", "${instance.object.id}");
	
//]]></script>

<div class="dashlet site-blog-list">
	<div class="title">Latest blog entries</div>
	<div class="body scrollableList" id="${args.htmlid}-body" <#if args.height??>style="height: ${args.height}px;"</#if>>
		<#list posts as post>
			<div class="detail-list-item <#if post_index == 0>first-item</#if> <#if !post_has_next>last-item</#if>">
				<h4><a href="${url.context}/page/site/${post.site}/blog-postview?container=blog&postId=${post.name}" class="theme-color-1 blog-post-title" title="${post.title}">${post.title}</a></h4>
	            <div class="post-details">Posted by <span class="theme-color-1">${post.postedBy}</span> on <span class="theme-color-1">${post.modifiedOn}</span> for site <a href="${url.context}/page/site/${post.site}/dashboard" class="theme-color-1">${post.site}</a></div>
	            <div class="description">${post.content}</div>
	    	</div>
		</#list>
	</div>
</div>