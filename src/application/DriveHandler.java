package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;

import model.BookmarkManager;
import model.DriveFile;
import model.DriveFileList;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

public class DriveHandler {
	private static final String DH_DRIVE_FILENAME = "bookmarks.dap";
	private static final String DH_DRIVE_FOLDERNAME = "DAP";
	private static String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
	private static HttpTransport httpTransport = new NetHttpTransport();
	private static JsonFactory jsonFactory = new JacksonFactory();
	private static GoogleClientSecrets clientSecrets;
	static {
		try {
			clientSecrets = GoogleClientSecrets.load(jsonFactory,
					new InputStreamReader(new FileInputStream(new java.io.File("client_secrets.json"))));
		} catch (IOException e) {
			clientSecrets = null;
		}
	}
	private Drive service;

	public DriveHandler(String refreshToken){
		if(refreshToken == null || refreshToken.isEmpty()) throw new IllegalArgumentException("No refresh token");
		try {
			GoogleCredential credential = getCredential(refreshToken);
			service = getService(credential);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public DriveFileList getFilelist(){
		try {
			DriveFileList files = new DriveFileList(service.files().list().setQ("trashed = false").execute());
			return files;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new DriveFileList();
	}
	public boolean hasFile(String filename){
		if(filename == null || filename.isEmpty()) return false;
		for(DriveFile file : getFilelist().getItems()){
			if(filename.equals(file.getTitle()))
				return true;
		}
		return false;
	}
	public DriveFile XcreateDAPFolder(){
		try {
			DriveFile body = new DriveFile();
			body.setTitle(DH_DRIVE_FOLDERNAME);
			body.setMimeType("application/vnd.google-apps.folder");
			body.setDescription("Container for all things DAP");
			DriveFile file = new DriveFile(service.files().insert(body.getFile()).execute());
			return file;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	public DriveFile createBookmarksFile(String data){
		try{
			java.io.File input_file = new java.io.File("bookmarks.dap");
			OutputStream out = new FileOutputStream(input_file);

			//Write
			out.write(data.getBytes());
			out.flush();
			out.close();

			//Create Google Drive file
			FileContent fileContent = new FileContent("text/plain", input_file);
			DriveFile body = new DriveFile();
			body.setTitle(DH_DRIVE_FILENAME);
			body.setMimeType("text/plain");
			body.setDescription("Bookmarks");

			DriveFile file = new DriveFile(service.files().insert(body.getFile(), fileContent).execute());
			return file;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	public String getContent(DriveFile file){
		String url = file.getDownloadUrl();
		try {
			HttpResponse resp = service.getRequestFactory().buildGetRequest(new GenericUrl(url)).execute();
			InputStream stream = resp.getContent();
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			
			StringBuilder sb = new StringBuilder();
			String line = in.readLine();
			while(line != null){
				sb.append(line);
				line = in.readLine();
			}
			String content = sb.toString();
			return content;
		} catch (IOException e) {
			return null;
		}
	}
	public DriveFile setContent(DriveFile file, String data){
		String fileId = file.getFileId();
		
		BookmarkManager.getInstance().saveBookmarks();
		
		//Create content
		try {
			File input_file = new File("bookmarks.dap");
//			OutputStream out = new FileOutputStream(input_file);

			System.out.println("DATA = "+data);
			
//			//Write
//			out.write(data.getBytes());
//			out.flush();
//			out.close();
			
			//Update Google Drive file
//			File fileContent = new File("dummy");
		    FileContent mediaContent = new FileContent("text/plain", input_file);
		    DriveFile result = new DriveFile(service.files().update(fileId, file.getFile(), mediaContent).execute());
		    return result;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	
	private Drive getService(GoogleCredential credential) throws IOException {
		Drive service = new Drive.Builder(httpTransport, jsonFactory, credential).setApplicationName("DAP").build();
		return service;
	}
	private GoogleCredential getCredential(String refreshToken) throws IOException{
		GoogleCredential credential = new GoogleCredential.Builder()
		.setTransport(httpTransport)
		.setJsonFactory(jsonFactory)
		.setClientSecrets(clientSecrets)
		.build();
		credential.setRefreshToken(refreshToken);
		credential.refreshToken();
		//		credential.getAccessToken();
		/* No point saving the access token. It doesn't last long anyway */
		return credential;
	}


	public static String getRefreshUrl() {
		if(clientSecrets == null) throw new IllegalArgumentException("Unable to read client client secrets statically");
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, jsonFactory, clientSecrets, Arrays.asList(DriveScopes.DRIVE_FILE))
		.setAccessType("offline")
		.setApprovalPrompt("auto")
		.build();


		String url = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
		//		System.out.println("Please open the following URL in your browser then type the authorization code:");
		//		System.out.println("  " + url);
		//		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		//		String code = br.readLine();



		return url;
	}
	public static String getRefreshToken(String code) {
		if(code == null || code.isEmpty()) throw new NullPointerException("No code => Unable to obtain refresh token");
		if(clientSecrets == null) throw new IllegalArgumentException("Unable to read client client secrets statically");
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, jsonFactory, clientSecrets, Arrays.asList(DriveScopes.DRIVE_FILE))
		.setAccessType("offline")
		.setApprovalPrompt("auto")
		.build();


		GoogleTokenResponse response;
		try {
			response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
			String refreshToken = response.getRefreshToken();
			return refreshToken;
		} catch (IOException e) {
			throw new NullPointerException("Unable to obtain refresh token");
		}
	}
}
