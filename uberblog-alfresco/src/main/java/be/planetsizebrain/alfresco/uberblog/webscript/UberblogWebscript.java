package be.planetsizebrain.alfresco.uberblog.webscript;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.query.PagingRequest;
import org.alfresco.query.PagingResults;
import org.alfresco.service.cmr.blog.BlogPostInfo;
import org.alfresco.service.cmr.blog.BlogService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.QName;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class UberblogWebscript extends AbstractWebScript {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
	private static final SimpleDateFormat DATE_FORMAT_RSS = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
	
	private AuthenticationService authService;
	private BlogService blogService;
	private FileFolderService fileFolderService;
	private NodeService nodeService;
	private SiteService siteService;
	private PersonService personService;
	private ContentService contentService;
	
	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		Calendar now = Calendar.getInstance();
		Calendar begin = Calendar.getInstance();
		begin.add(Calendar.MONTH, -1);
		
		String username = authService.getCurrentUserName();
		
		List<Map<String, String>> blogposts = new ArrayList<Map<String, String>>();
		List<SiteInfo> sites = siteService.listSites(username);
		for (SiteInfo siteInfo : sites) {
			NodeRef site = siteInfo.getNodeRef();
			List<FileInfo> folders = fileFolderService.listFolders(site);

			for (FileInfo fileInfo : folders) {
				if (fileInfo.isFolder() && fileInfo.getName().contains("blog")) {
					int counter = 0;
					PagingResults<BlogPostInfo> posts = null;
					do {
						PagingRequest pg = new PagingRequest(counter, 10);
						posts = blogService.getPublished(fileInfo.getNodeRef(), begin.getTime(), now.getTime(), null, pg);
						counter += 10;
						
						for (BlogPostInfo p : posts.getPage()) {
							Map<QName, Serializable> props = nodeService.getProperties(p.getNodeRef());
							Map<String, String> post = new HashMap<String, String>();
							
							String name = (String) props.get(QName.createQName("http://www.alfresco.org/model/content/1.0", "name"));
							post.put("name", name);
							
							post.put("site", siteInfo.getShortName());
							
							String title = (String) props.get(QName.createQName("http://www.alfresco.org/model/content/1.0", "title"));
							post.put("title", title);
							
							String content = contentService.getReader(p.getNodeRef(), ContentModel.PROP_CONTENT).getContentString();
							content = content.replaceAll("\\<.*?\\>", "");
							if (content.length() > 100) {
								content = content.substring(0, 101);
								int lastSpace = content.lastIndexOf(' ');
								if (lastSpace > 0) {
									content = content.substring(0, lastSpace) + "...";
								}
							}
							post.put("content", content);
							
							String poster = (String) props.get(QName.createQName("http://www.alfresco.org/model/content/1.0", "creator"));
							NodeRef person = personService.getPerson(poster);
							Map<QName, Serializable> pprops = nodeService.getProperties(person);
							String firstName = (String) pprops.get(QName.createQName("http://www.alfresco.org/model/content/1.0", "firstName"));
							String lastName = (String) pprops.get(QName.createQName("http://www.alfresco.org/model/content/1.0", "lastName"));
							String email = (String) pprops.get(QName.createQName("http://www.alfresco.org/model/content/1.0", "email"));	
							String fullName = (firstName.equals("") ? "" : firstName) + (lastName.equals("") ? "" : (" " + lastName));
							
							post.put("postedBy", fullName);
							post.put("postedByRssNotation", email + " (" + fullName + ")");
							
							Date modifiedOn = (Date) props.get(QName.createQName("http://www.alfresco.org/model/content/1.0", "modified"));
							post.put("modifiedOn", DATE_FORMAT.format(modifiedOn));
							post.put("modifiedOnRssNotation", DATE_FORMAT_RSS.format(modifiedOn) + " Z");
							
							blogposts.add(post);
						}
					} while (posts.hasMoreItems());
				}
			}
		}
		
		Collections.sort(blogposts, new Comparator<Map<String, String>>() {
			@Override
			public int compare(Map<String, String> o1, Map<String, String> o2) {
				try {
					Date d1 = DATE_FORMAT.parse(o1.get("modifiedOn"));
					Date d2 = DATE_FORMAT.parse(o2.get("modifiedOn"));
					
					// Reverse compare, younger dates on top
					return d2.compareTo(d1);
				} catch (ParseException pe) {
					// log?
					return 0;
				}
			}
		});
		
		JSONObject obj = new JSONObject();
    	try {
			obj.put("posts", blogposts);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	String jsonString = obj.toString();
    	res.getWriter().write(jsonString);
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authService = authenticationService;
	}
	
	public void setBlogService(BlogService blogService) {
		this.blogService = blogService;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}
	
	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}
	
	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}
	
	public void setFileFolderService(FileFolderService fileFolderService) {
		this.fileFolderService = fileFolderService;
	}
}