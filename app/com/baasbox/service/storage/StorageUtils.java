package com.baasbox.service.storage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.imgscalr.Scalr;

import com.baasbox.dao.FileAssetDao;
import com.baasbox.dao.FileDao;
import com.baasbox.dao.GenericDao;
import com.baasbox.dao.NodeDao;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.DocumentIsNotAFileException;
import com.baasbox.exception.InvalidSizePatternException;
import com.google.common.collect.ImmutableSet;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class StorageUtils {
	public enum WritebleImageFormat {
		png,jpg,bmp,gif,jpeg
	}
	
	public static class ImageDimensions{
		int width;
		int height;
		boolean widthInPixel;
		boolean heightInPixel;
		boolean maxDimension;
		
		@Override
		public String toString(){
			return width + (widthInPixel?"":"%") + "-" + height + (heightInPixel?"":"%");
		}
	}
	
	public static ImmutableSet<String> fileClasses = ImmutableSet.of(FileAssetDao.MODEL_NAME,FileDao.MODEL_NAME);
	
	private static byte[] resizeImage(BufferedImage bufferedImage, WritebleImageFormat format, int width,int height) throws IOException{
		BufferedImage thumbnail = Scalr.resize(bufferedImage,Scalr.Method.ULTRA_QUALITY,Scalr.Mode.FIT_EXACT,width,height);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(thumbnail, format.name(), output);
		return output.toByteArray();
	}//convertSizeToDimensions
	
	/**
	 * 
	 * @param bytes
	 * @param format
	 * @param size one of the format allowed in ImagesConfigurationEnum.IMAGE_ALLOWED_AUTOMATIC_RESIZE_FORMATS
	 * @return
	 * @throws IOException
	 * @throws InvalidSizePatternException 
	 */
	public static byte[] resizeImage(byte[] bytes, WritebleImageFormat format, ImageDimensions imgDim) throws IOException{
		ByteArrayInputStream bytesStream = new ByteArrayInputStream(bytes);
		BufferedImage bufferedImage = ImageIO.read(bytesStream);
		int destWidth=imgDim.width;
		int destHeight=imgDim.height;
		if (imgDim.maxDimension){
			int origWidth=bufferedImage.getWidth();
			int origHeight=bufferedImage.getHeight();
			if (origWidth>imgDim.width || origHeight>imgDim.height){
				if (origWidth>origHeight){
					destWidth=imgDim.width;
					destHeight=(origHeight*destWidth)/origWidth;
				}else{
					destHeight=imgDim.height;
					destWidth=(origWidth*destHeight)/origHeight;
				}
			}else{
				destWidth=origWidth;
				destHeight=origHeight;
			}
		}else{
			if (!imgDim.heightInPixel || !imgDim.widthInPixel){
				int origWidth=bufferedImage.getWidth();
				int origHeight=bufferedImage.getHeight();
				if (!imgDim.widthInPixel) destWidth = origWidth * imgDim.width / 100;
				if (!imgDim.heightInPixel) destHeight = origHeight * imgDim.height / 100;
			}
		}
		return resizeImage(bufferedImage, format, destWidth, destHeight);
	}
	
	public static ImageDimensions convertWidthHeightToDimension(String w, String h) throws InvalidSizePatternException{
		return  convertPatternToDimensions (w+"-"+h);
	}
	
	/**
	 * Extracts width and height from the size in the String format
	 * @param sizePattern the string pattern could be nn%, 24px-34px, 24%-23px, 345-456
	 * @return an instance of ImageDimensions
	 * @throws InvalidSizePatternException 
	 */
	public static ImageDimensions convertPatternToDimensions(final String sizePattern) throws InvalidSizePatternException{
		ImageDimensions imgDim = new ImageDimensions();
		String regexp = "\\d+%";
		if (sizePattern.trim().matches(regexp)) { //size in the form 58%, then w and h are equals and expressed in %
			int value=Integer.parseInt(sizePattern.substring(0, sizePattern.length()-1));
			imgDim.height=value;
			imgDim.width=value;
			imgDim.heightInPixel=false;
			imgDim.widthInPixel=false;
			imgDim.maxDimension=false;
			return imgDim;
		}
		regexp = "<=\\d+px";
		if (sizePattern.trim().matches(regexp)) { //size in the form <=58px, then w and h will be at max 58px each
			int value=Integer.parseInt(sizePattern.substring(2, sizePattern.length()-2));
			imgDim.height=value;
			imgDim.width=value;
			imgDim.heightInPixel=false;
			imgDim.widthInPixel=false;
			imgDim.maxDimension=true;
			return imgDim;
		}
		//guess if size is in the form 123px-256px  where w=123 pixels and h=256 pixels or in the form 25%-30% or mixed
		String[] wandh = sizePattern.split("-");
		try{
			if (wandh.length==2){
				//width
				String width = wandh[0];
				if (width.endsWith("%")){
					imgDim.width=Integer.parseInt(width.substring(0, width.length()));
					imgDim.widthInPixel=false;
				}else if (width.endsWith("px")){
					imgDim.width=Integer.parseInt(width.substring(0, width.length()-1));
					imgDim.widthInPixel=true;
				}else {
					imgDim.width=Integer.parseInt(width);
					imgDim.widthInPixel=true;				
				}
				//height
				String height = wandh[1];
				if (width.endsWith("%")){
					imgDim.height=Integer.parseInt(height.substring(0, height.length()));
					imgDim.heightInPixel=false;
				}else if (width.endsWith("px")){
					imgDim.height=Integer.parseInt(height.substring(0, height.length()-1));
					imgDim.heightInPixel=true;
				}else {
					imgDim.height=Integer.parseInt(height);
					imgDim.heightInPixel=true;				
				}		
				return imgDim;
			}
		}catch (Throwable e){
			throw new InvalidSizePatternException(sizePattern + " is not valid ", e);
		}
		throw new InvalidSizePatternException(sizePattern + " is not valid: missing the '-' character between the width and the height");
	}


	public static ByteArrayOutputStream extractFileFromDoc(ODocument doc) throws DocumentIsNotAFileException, IOException{
		if (!docIsAFile(doc)) throw new DocumentIsNotAFileException();
		if (!doc.containsField("file")) throw new DocumentIsNotAFileException("the file field does not exist");
		ORecordBytes record = null;
		try {
			record = doc.field("file");
		}catch (Exception e){
			throw new DocumentIsNotAFileException("The file field exists but does not contains a valid file");
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		record.toOutputStream(out);
		return out;
	}
	
	public static boolean docIsAFile(ODocument doc){
		String className=doc.getClassName();
		return fileClasses.contains(className);
	}
	
	public static boolean docIsAnImage(ODocument doc) {
		if (!docIsAFile(doc)) return false;
		String contentType = doc.field("contentType");
		if (contentType==null || contentType.isEmpty()) return false;
		return (contentType.startsWith("image/"));
	}
	
	public static OrientVertex getNodeVertex(String nodeId) throws DocumentNotFoundException{
		GenericDao dao = GenericDao.getInstance();
		OrientGraph conn = DbHelper.getOrientGraphConnection();
		ORID nodeORID = dao.getRidNodeByUUID(nodeId);
		if (nodeORID==null) throw new DocumentNotFoundException(nodeId + " is not a valid Id");
		ODocument nodeDoc = dao.get(nodeORID);
		if (nodeDoc==null) throw new DocumentNotFoundException(nodeId + " is not a valid Id");
		return conn.getVertex(nodeDoc.field(NodeDao.FIELD_LINK_TO_VERTEX));
	}
}
