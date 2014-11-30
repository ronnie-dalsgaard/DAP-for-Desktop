package application;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;

import support.ConflictResolver;
import support.iConflictResolver;

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
	private String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
	private HttpTransport httpTransport = new NetHttpTransport();
	private JsonFactory jsonFactory = new JacksonFactory();
	private GoogleClientSecrets clientSecrets;
	private Drive drive;

	public static void main(String[] args) throws IOException {
		DriveHandler handler = new DriveHandler();
		FileList files = handler.getFilelist();
		for(File file : files.getItems()){
			System.out.println(file.toPrettyString() + "\n====================================\n");
		}
		System.out.println("DONE");
	}


	public DriveHandler(){
		try {
			clientSecrets = GoogleClientSecrets.load(jsonFactory,
					new InputStreamReader(new FileInputStream(new java.io.File("client_secrets.json"))));

			//FIXME
			/* Refresh token generated aprox. 15:58 24/11-2014 */
			String refreshToken = null;
			refreshToken = "1/4u5CoBsbsS-K2WBJnP2JDtMLXUkJ7emG4Xyk8p1pN2QMEudVrK5jSpoR30zcRFq6";
			if(refreshToken == null || refreshToken.isEmpty()){
				refreshToken = getRefreshToken();
				System.out.println("Refresh token: "+refreshToken);
			}

			GoogleCredential credential = getCredential(refreshToken);
			drive = getService(credential);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	public void start(){
		java.io.File input_file = new java.io.File("document.txt");
		FileContent fileContent = new FileContent("text/plain", input_file);

		try {


			//Insert a file  
			File body = new File();
			body.setTitle("My document");
			body.setDescription("A test document");
			body.setMimeType("text/plain");


			File file = drive.files().insert(body, fileContent).execute();
			System.out.println("File ID: " + file.getId());



		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Done");
	}


	private FileList getFilelist(){
		try {
			FileList files = drive.files().list().setQ("trashed = false").execute();
			return files;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new FileList();
	}
	private boolean hasFile(String filename){
		if(filename == null || filename.isEmpty()) return false;
		for(File file : getFilelist().getItems()){
			if(filename.equals(file.getTitle()))
				return true;
		}
		return false;
	}
	private File createDAPFolder(){
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
	private File createBookmarksFile(String oldData, String newData){
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
		String accessToken = credential.getAccessToken();
		//		System.out.println("Access Token = " + accessToken);
		return credential;
	}
	private String getRefreshToken() throws IOException {
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, jsonFactory, clientSecrets, Arrays.asList(DriveScopes.DRIVE_FILE))
		.setAccessType("offline")
		.setApprovalPrompt("auto")
		.build();


		String url = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
		System.out.println("Please open the following URL in your browser then type the authorization code:");
		System.out.println("  " + url);
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String code = br.readLine();

		GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
		String refreshToken = response.getRefreshToken();

		return refreshToken;
	}
}