package com.guesswhat.server.services.rs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

import org.apache.commons.io.IOUtils;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;
import com.google.apphosting.api.ApiProxy.RequestTooLargeException;
import com.guesswhat.server.persistence.jpa.cfg.EntityFactory;
import com.guesswhat.server.persistence.jpa.entity.Image;
import com.guesswhat.server.persistence.jpa.entity.ImageHolder;
import com.guesswhat.server.persistence.jpa.entity.Question;
import com.guesswhat.server.persistence.jpa.entity.QuestionIncubator;
import com.guesswhat.server.services.rs.dto.ImageType;
import com.guesswhat.server.services.rs.face.ImageService;

@Path("/images")
public class ImageServiceImpl implements ImageService {

	@Override
	@RolesAllowed("WRITER")
	public void createQuestionImage(Long questionId, String imageType, HttpServletRequest request, InputStream fileInputStream) {
		QuestionIncubator questionIncubator = EntityFactory.getInstance().getQuestionIncubatorDAO().find(questionId);
		ImageHolder imageHolder = null;
		if (questionIncubator.getImageQuestion() == null) {
			imageHolder = new ImageHolder();
			EntityFactory.getInstance().getImageHolderDAO().save(imageHolder);
			questionIncubator.setImageQuestion(imageHolder.getKey());
			EntityFactory.getInstance().getQuestionIncubatorDAO().update(questionIncubator);
		} else {
			imageHolder = EntityFactory.getInstance().getImageHolderDAO().find(questionIncubator.getImageQuestion());
		}
		if (buildImageHolder(imageHolder, imageType, fileInputStream)) {
			EntityFactory.getInstance().getImageHolderDAO().update(imageHolder);
		}
		if (!buildQuestion(questionIncubator)) {
			EntityFactory.getInstance().getQuestionIncubatorDAO().update(questionIncubator);
		}
	}
	
	@Override
	@RolesAllowed("WRITER")
	public void createAnswerImage(Long questionId, String imageType, HttpServletRequest request, InputStream fileInputStream) {
		QuestionIncubator questionIncubator = EntityFactory.getInstance().getQuestionIncubatorDAO().find(questionId);
		ImageHolder imageHolder = null;
		if (questionIncubator.getImageAnswer() == null) {
			imageHolder = new ImageHolder();
			EntityFactory.getInstance().getImageHolderDAO().save(imageHolder);
			questionIncubator.setImageAnswer(imageHolder.getKey());
			EntityFactory.getInstance().getQuestionIncubatorDAO().update(questionIncubator);
		} else {
			imageHolder = EntityFactory.getInstance().getImageHolderDAO().find(questionIncubator.getImageAnswer());
		}
		if (buildImageHolder(imageHolder, imageType, fileInputStream)) {
			EntityFactory.getInstance().getImageHolderDAO().update(imageHolder);
		}
		if (!buildQuestion(questionIncubator)) {
			EntityFactory.getInstance().getQuestionIncubatorDAO().update(questionIncubator);
		}
	}

	private boolean buildImageHolder(ImageHolder imageHolder, String imageType, InputStream source) {
	    Blob blob = null;
	    try {
	    	byte[] bytes = IOUtils.toByteArray(source);
			blob = new Blob(bytes);
		
		    Image image = new Image(blob);
		    
		    try {
		    	EntityFactory.getInstance().getImageDAO().save(image);
		    } catch (RequestTooLargeException e) {
		    	// file is too big. Workaround:
				int middle = bytes.length / 2;
				byte[] bytes1 = Arrays.copyOfRange(bytes, 0, middle);
				byte[] bytes2 = Arrays.copyOfRange(bytes, middle,  bytes.length);
				Blob blob1 = new Blob(bytes1);
				Blob blob2 = new Blob(bytes2);
				image = new Image(blob1);
				Image image2 = new Image(blob2);
				EntityFactory.getInstance().getImageDAO().save(image2);
				image.setSecondPart(image2.getKey().getId());
				// try again :)
				EntityFactory.getInstance().getImageDAO().save(image);
		    }
		    Key imageKey = image.getKey();
		    
		    switch(ImageType.valueOf(imageType)) {
		    	case XXHDPI:	imageHolder.setXxhdpiImage(imageKey);
		    					break;
		    	case XHDPI:		imageHolder.setXhdpiImage(imageKey);
								break;
		    	case HDPI:		imageHolder.setHdpiImage(imageKey);
								break;
		    	case MDPI:		imageHolder.setMdpiImage(imageKey);
								break;
		    	case LDPI:		imageHolder.setLdpiImage(imageKey);
								break;
				default:		return false;
		    }
		    
		    return true;
		    
	    } catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private boolean buildQuestion(QuestionIncubator questionIncubator) {
		if (questionIncubator.getImageQuestion() != null && questionIncubator.getImageAnswer() != null) {
			ImageHolder imageHolderQuestion = EntityFactory.getInstance().getImageHolderDAO().find(questionIncubator.getImageQuestion());
			ImageHolder imageHolderAnswer = EntityFactory.getInstance().getImageHolderDAO().find(questionIncubator.getImageAnswer());
			if (imageHolderQuestion.isFull() && imageHolderAnswer.isFull()) {
				Question question = new Question(questionIncubator);
				EntityFactory.getInstance().getQuestionDAO().save(question);
				EntityFactory.getInstance().getQuestionIncubatorDAO().remove(questionIncubator.getKey());
				return true ;
			}
		}
		
		return false;		
	}

	@Override
	@RolesAllowed("READER")
	public String findQuestionImage(Long questionId, String imageType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@RolesAllowed("READER")
	public String findAnswerImage(Long questionId, String imageType) {
		// TODO Auto-generated method stub
		return null;
	}

}
