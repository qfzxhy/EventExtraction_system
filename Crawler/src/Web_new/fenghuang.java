package Web_new;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

import Common.DBtools;
import Common.News;
//import Java_EventDetection_News.Label.LabelItem;
//import Java_EventDetection_News.Program.RunDetection;
public class fenghuang {
	DBtools dbtool = new DBtools();
	public static String NEW_IMG_PATH = "";
	
//	public static RunDetection runDetection = null;
	public void find_information(String u) throws SQLException, ParseException, IOException{
		System.out.println("next"+u);
				String title=new String();
				String time=new String();
				StringBuilder mainbody=new StringBuilder();
				String source=new String();
				String imageUrl=new String();
				StringBuilder newImageName=new StringBuilder();
				URL url;
				News nInsert =new News();
				try {
					System.setProperty("sun.net.client.defaultConnectTimeout", "3000");
					System.setProperty("sun.net.client.defaultReadTimeout", "3000");
					url = new URL(u);
					URLConnection context =url.openConnection();
					InputStream input=context.getInputStream();
					BufferedReader read=new BufferedReader(new InputStreamReader(input,"UTF-8"));
				
					String s;
					while((s = read.readLine()) != null){
						//标题
						if(s.matches(".*<title>.*")){
							String xs=s.substring(s.indexOf(">")+1, s.indexOf("</title>"));
							if(xs.indexOf('|')>=0)
								title=xs.substring(0,xs.indexOf("|"));
							else
								title=xs.substring(0,xs.indexOf("_"));
							source=xs.substring(xs.lastIndexOf("_")+1);					
						}
						//时间
						if(s.matches(".*datePublished.*")){
							String ti=s.substring(s.indexOf(">")+1,s.indexOf("<",s.indexOf(">")));
							time=ti.substring(0,4)+"-"+ti.substring(5,7)+"-"+ti.substring(8,10)+ti.substring(11)+":00";
						}		
						//正文
						if(s.matches(".*<div id=\"artical_real\".*")){
							int flag=0;
							while(!(s=read.readLine()).matches(".*全部文章内容区域.*")&&flag!=1){
								if(s.matches(".*<p.*")&&s.length()>10){
									if(s.matches(".*<p.*")){	
										mainbody.append(getmainbody(s));
									}
									//图片
									if(s.matches(".*<p.*")&&s.matches(".*img.*")){
										newImageName.append(getimageUrlpath(s,u)+"\n");
									}							
								}			
								if(s.matches(".*下一页.*"))
									break;
								
								if(s.matches(".*ifengLogo.*"))
									flag=1;
							}
							
						}										
						
						//来源
						if(s.matches(".*publisher.*")){
							if(s.indexOf("nofollow\">")>=0)
								source=s.substring(s.indexOf("nofollow\">")+10,s.indexOf("<",s.indexOf("nofollow\">")));
							else
								source=s.substring(s.indexOf("class=\"ss03\">")+13,s.indexOf("<",s.indexOf("class=\"ss03\">")));
						}
						
						if(s.matches(".*下一页.*")&&s.matches(".*div_page_roll1.*")){

							String newu= new String();
							newu=s.substring(s.lastIndexOf("http",s.indexOf("下一页")), s.lastIndexOf("htm",s.indexOf("下一页"))+3);
							find_information(newu);
						}
										
					}
					
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
				java.util.Date date=new java.util.Date();
				java.sql.Timestamp time1=new java.sql.Timestamp(date.getTime());
				
				nInsert.setTitle(title.toString());
				nInsert.setAgent(source);
				if(!time.matches(""))
					nInsert.setUpdateTime(time);
				else
					nInsert.setUpdateTime(time1.toString());
				nInsert.setContent(mainbody.toString());
				nInsert.setPicture(newImageName.toString());
				nInsert.setUrl(u);
				nInsert.setSaveTime(time1.toString());
				
				System.out.println("url "+nInsert.getUrl());
				System.out.println("title "+nInsert.getTitle());
				System.out.println("time "+nInsert.getUpdateTime());
				System.out.println("mainbody "+nInsert.getContent());
				System.out.println("imageUrl "+nInsert.getPicture());
				System.out.println("source "+nInsert.getAgent());
				System.out.println("searchtime "+nInsert.getSaveTime());
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("news", true)));
				if(nInsert.getTitle().length() > 3)
					bw.write(nInsert.getTitle()+"\n");
				bw.close();
//				if(nInsert.getUrl()==null||nInsert.getTitle().equalsIgnoreCase("")||nInsert.getTitle()==null
//						||nInsert.getContent().equalsIgnoreCase("")||nInsert.getContent()==null){
//					System.out.println("empty or no url");
//					return;
//				}
				int newsId = dbtool.selectMaxIdfenghuang()+1;
				if(newsId>=0)
				{
					dbtool.deleteExcessFenghuang("fenghuang", newsId);
//					LabelItem extractResult = runDetection.GetEventInforfromNews("fenghuang", String.valueOf(newsId), nInsert.getUpdateTime(), nInsert.getTitle());
//					extractResult.Print();
//					runDetection.labelDB.AddLabeltoTempTable(extractResult);
				}
				dbtool.insertValuesfenghuang(nInsert);
			}
	
	public StringBuilder getmainbody(String s){/*	获取正文	*/
		StringBuilder mainbody=new StringBuilder();
		int a=0;
		int b=0;
		while(a>=0&&b>=0){
			a=s.indexOf(">",b);
			b=s.indexOf("<",a);

			if(b-a>1){

				mainbody.append(s.substring(a+1, b));	
			}		
		}				
		mainbody.append("\n");
		return mainbody;
	}
	
	public String getimageUrlpath(String s,String u) throws IOException{/*	获取图片，并下载存储在本地	*/
		String im=new String();
		String imageUrl=new String();
		if(s.matches(".*http.*")){
			if(s.matches(".*\\.jpg.*")){
				imageUrl=s.substring(s.indexOf("src=")+5,s.indexOf(".jpg")+4);			
			}
			else if(s.matches(".*\\.JPG.*")){
				imageUrl=s.substring(s.indexOf("src=")+5,s.indexOf(".JPG")+4);			
			}
			else if(s.matches(".*\\.png.*")){
				imageUrl=s.substring(s.indexOf("src=")+5,s.indexOf(".png")+4);						
			}
			else if(s.matches(".*\\.PNG.*")){
				imageUrl=s.substring(s.indexOf("src=")+5,s.indexOf(".PNG")+4);						
			}
			else if(s.matches(".*\\.bmp.*")){
				imageUrl=s.substring(s.indexOf("src=")+5,s.indexOf(".bmp")+4);						
			}
			else if(s.matches(".*\\.BMP.*")){
				imageUrl=s.substring(s.indexOf("src=")+5,s.indexOf(".BMP")+4);						
			}
			else if(s.matches(".*\\.gif.*")){
				imageUrl=s.substring(s.indexOf("src=")+5,s.indexOf(".gif")+4);						
			}
			else if(s.matches(".*\\.GIF.*")){
				imageUrl=s.substring(s.indexOf("src=")+5,s.indexOf(".GIF")+4);						
			}
		}
		else if(s.matches(".*\\.jpg.*")){
			im=s.substring(s.indexOf("src=")+5,s.indexOf(".jpg")+4);
			imageUrl = u.substring(0, u.lastIndexOf("/")+1)+im;
		}
		else if(s.matches(".*\\.JPG.*")){
			im=s.substring(s.indexOf("src=")+5,s.indexOf(".JPG")+4);
			imageUrl = u.substring(0, u.lastIndexOf("/")+1)+im;
		}
		else if(s.matches(".*\\.png.*")){
			im=s.substring(s.indexOf("src=")+5,s.indexOf(".png")+4);
			imageUrl = u.substring(0, u.lastIndexOf("/")+1)+im;
		}
		else if(s.matches(".*\\.PNG.*")){
			im=s.substring(s.indexOf("src=")+5,s.indexOf(".PNG")+4);
			imageUrl = u.substring(0, u.lastIndexOf("/")+1)+im;
		}
		else if(s.matches(".*\\.bmp.*")){
			im=s.substring(s.indexOf("src=")+5,s.indexOf(".bmp")+4);
			imageUrl = u.substring(0, u.lastIndexOf("/")+1)+im;
		}
		else if(s.matches(".*\\.BMP.*")){
			im=s.substring(s.indexOf("src=")+5,s.indexOf(".BMP")+4);
			imageUrl = u.substring(0, u.lastIndexOf("/")+1)+im;
		}
		else if(s.matches(".*\\.gif.*")){
			im=s.substring(s.indexOf("src=")+5,s.indexOf(".gif")+4);
			imageUrl = u.substring(0, u.lastIndexOf("/")+1)+im;
		}
		else if(s.matches(".*\\.GIF.*")){
			im=s.substring(s.indexOf("src=")+5,s.indexOf(".GIF")+4);
			imageUrl = u.substring(0, u.lastIndexOf("/")+1)+im;
		}
		
		URL iurl = new URL(imageUrl);

		DataInputStream dis = new DataInputStream(iurl.openStream());
			    
		String newImagepath="D:/shangd/workspace/Web_new/xinhua_img/"+imageUrl.substring(imageUrl.lastIndexOf("/"));
			    
		FileOutputStream fos = new FileOutputStream(new File(newImagepath));
		byte[] buffer = new byte[1024];
			    
		int length;
		while((length = dis.read(buffer))>0){
			fos.write(buffer,0,length);							    
		}
			    
		dis.close();
			    
		fos.close();
		
		return newImagepath;
	}
	
}

