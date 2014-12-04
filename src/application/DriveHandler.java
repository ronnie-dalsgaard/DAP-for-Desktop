package application;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;

import model.DriveFile;
import model.DriveFileList;
import support.ConflictResolver;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;


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
	private Drive drive;

	public DriveHandler(String refreshToken){
		if(refreshToken == null || refreshToken.isEmpty()) throw new IllegalArgumentException("No refresh token");
		try {
			GoogleCredential credential = getCredential(refreshToken);
			drive = getService(credential);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public DriveFileList getFilelist(){
		try {
			FileList list = drive.files().list().setQ("trashed = false").execute();
			DriveFileList files = new DriveFileList(list);
			return files;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new DriveFileList(new FileList());
	}
	public boolean hasFile(String filename){
		if(filename == null || filename.isEmpty()) return false;
		for(DriveFile file : getFilelist().getItems()){
			if(filename.equals(file.getTitle()))
				return true;
		}
		return false;
	}
	public File createDAPFolder(){
		try {
			File body = new File();
			body.setTitle(DH_DRIVE_FOLDERNAME);
			body.setMimeType("application/vnd.google-apps.folder");
			body.setDescription("Container for all things DAP");
			File file = drive.files().insert(body).execute();
			return file;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	public File createBookmarksFile(String oldData, String newData){
		try{
			java.io.File input_file = new java.io.File("bookmarks.dap");
			OutputStream out = new FileOutputStream(input_file);

			//Resove conflicts
			ConflictResolver resolver = new ConflictResolver();
			String resolved = resolver.resolveConflicts(oldData, newData);

			//Write
			out.write(resolved.getBytes());
			out.flush();
			out.close();

			//Create Google Drive file
			FileContent fileContent = new FileContent("text/plain", input_file);
			File body = new File();
			body.setTitle(DH_DRIVE_FILENAME);
			body.setMimeType("text/plain");
			body.setDescription("Bookmarks");

			File file = drive.files().insert(body, fileContent).execute();
			return file;

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

//public void start(){
//java.io.File input_file = new java.io.File("document.txt");
//FileContent fileContent = new FileContent("text/plain", input_file);
//
//try {
//
//
//	//Insert a file  
//	File body = new File();
//	body.setTitle("My document");
//	body.setDescription("A test document");
//	body.setMimeType("text/plain");
//
//
//	File file = drive.files().insert(body, fileContent).execute();
//	System.out.println("File ID: " + file.getId());
//
//
//
//} catch (IOException e) {
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//}
//
//System.out.println("Done");
//}