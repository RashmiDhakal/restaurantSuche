/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.amazon.customskill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.customskill.exceptions.DeviceAddressClientException;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.Context;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Permissions;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.speechlet.interfaces.system.SystemInterface;
import com.amazon.speech.speechlet.interfaces.system.SystemState;
import com.amazon.speech.ui.AskForPermissionsConsentCard;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SsmlOutputSpeech;

import nlp.dkpro.backend.PosTagger;

/*
 * This class is the actual skill. Here you receive the input and have to produce the speech output. 
 */
public class AlexaSkillSpeechlet implements SpeechletV2 {
	
	private static final String ADDRESS_CARD_TITLE = "Ich darf deine Adresse nicht lesen";

    /**
     * The permissions that this skill relies on for retrieving addresses. If the consent token isn't
     * available or invalid, we will request the user to grant us the following permission
     * via a permission card.
     *
     * Another Possible value if you only want permissions for the country and postal code is:
     * read::alexa:device:all:address:country_and_postal_code
     * Be sure to check your permissions settings for your skill on https://developer.amazon.com/
     */
    private static final String ALL_ADDRESS_PERMISSION = "read::alexa:device:all:address";

    private static final String WELCOME_TEXT = "Welcome to the Sample Device Address API Skill! What do you want to ask?";
    private static final String HELP_TEXT = "You can use this skill by asking something like: whats my address";
    private static final String UNHANDLED_TEXT = "This is unsupported. Please ask something else.";
    private static final String ERROR_TEXT = "There was an error with the skill. Please try again.";
	
	
	public static String userRequest;
	public String address;

	static Logger logger = LoggerFactory.getLogger(AlexaSkillSpeechlet.class);

	@Override
	public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
		logger.info("Alexa session begins");
	}

	@Override
	public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
		return getWelcomeResponse();
	}
	
	private SystemState getSystemState(Context context) {
        return context.getState(SystemInterface.class, SystemState.class);
    }
	
	/**
     * Helper method for retrieving an OutputSpeech object when given a string of TTS.
     * @param speechText the text that should be spoken out to the user.
     * @return an instance of SpeechOutput.
     */
    private PlainTextOutputSpeech getPlainTextOutputSpeech(String speechText) {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return speech;
    }
	
	/**
     * Creates a {@code SpeechletResponse} for permission requests.
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getPermissionsResponse() {
        String speechText = "Ich bräuchte eine Erlaubnis deine Adressdaten zu benutzen um die nächsten Fressbuden zu finden. "
        		+ "Würdest du mir diese bitte geben?"
        		+ "Die entsprechende Anfrage schicke ich dir gleich.";

        // Create the permission card content.
        // The differences between a permissions card and a simple card is that the
        // permissions card includes additional indicators for a user to enable permissions if needed.
        AskForPermissionsConsentCard card = new AskForPermissionsConsentCard();
        card.setTitle(ADDRESS_CARD_TITLE);

        Set<String> permissions = new HashSet<>();
        permissions.add(ALL_ADDRESS_PERMISSION);
        card.setPermissions(permissions);

        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);

        return SpeechletResponse.newTellResponse(speech, card);
    }

	@Override
	public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
		IntentRequest request = requestEnvelope.getRequest();
		
		Intent intent = request.getIntent();
		String response = "";
		try {
			Session session = requestEnvelope.getSession();
			Permissions permissions = session.getUser().getPermissions();
	        if (permissions == null) {
	            return getPermissionsResponse();
	        }
		SystemState systemState = getSystemState(requestEnvelope.getContext());
        String apiAccessToken = systemState.getApiAccessToken();
        String deviceId = systemState.getDevice().getDeviceId();
        String apiEndpoint = systemState.getApiEndpoint();
        AlexaDeviceAddressClient alexaDeviceAddressClient = new AlexaDeviceAddressClient(
                deviceId, apiAccessToken, apiEndpoint);

        
			Address addressObject = alexaDeviceAddressClient.getFullAddress();
			address = addressObject.getCity() + ", " + addressObject.getAddressLine1() + " " + addressObject.getAddressLine2(); 
			System.out.println("ADDRESS RECEIVED: " + address);
		} catch (DeviceAddressClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Map<String, Slot> slots = intent.getSlots();
		
		if (slots.get("restaurant").getValue() != null) { 
			//if (intent.getSlots().containsKey("restaurant")) { 
			System.out.println("Restaurant");
			ArrayList<Restaurant> restaurants = App.getData(address);
			for(Restaurant restaurant : restaurants) {
				if(intent.getSlot("restaurant").getValue().contains(restaurant.getName().toLowerCase())) {
					response = "In der Nähe gibt es " + restaurant.getName() + ". Die Adresse ist " + restaurant.getAddress();
					return continueConversation(response);
				}
			}
			
		} else if (slots.get("gericht").getValue() != null) {
		//} else if (intent.getSlots().containsKey("gericht")) {
			System.out.println("Gericht");
			ArrayList<Restaurant> restaurants = App.getData(address);
			System.out.println("Data " + (restaurants == null));
			System.out.println("Data " + (restaurants.isEmpty()));
			for(Restaurant restaurant : restaurants) {
				System.out.println("Title " + (restaurant.getTitle()));
				System.out.println("Intent " + (intent.getSlot("gericht").getValue()));
				for (String title : restaurant.getTitle()) {
					if(intent.getSlot("gericht").getValue().contains(title.toLowerCase())) {
						response = "In der Nähe gibt es " + restaurant.getName() + ", wo du " + title + " essen kanst.";
						return continueConversation(response);
					}
				}
				
			}
			
		} else if (slots.get("noidea").getValue() != null) { 	
			return continueConversation("keine Ahnung");
		}
		
		//return continueConversation("Möchtest du was bestimmtes essen oder ein bestimmtes Restaurant besuchen?");
		return continueConversation("ok");
		
		

		//userRequest = intent.getSlot("Alles").getValue();
		
	/*	logger.info("Received following text: [" + userRequest + "]");
		if (userRequest.contains("ja")) {
			String s = App.getData().get(1);
			return response(s);
		} else {
			return response("Auf wiederhören!");
		}
		*/
		
		//return response("Erkannter Text: " + userRequest);
//        return responseWithFlavour("Erkannte Nomen: " + result, new Random().nextInt(5));
	}

	/**
	 * formats the text in weird ways
	 * 
	 * @param text
	 * @param i
	 * @return
	 */
	private SpeechletResponse responseWithFlavour(String text, int i) {

		SsmlOutputSpeech speech = new SsmlOutputSpeech();
		switch (i) {
		case 0:
			speech.setSsml("<speak><amazon:effect name=\"whispered\">" + text + "</amazon:effect></speak>");
			break;
		case 1:
			speech.setSsml("<speak><emphasis level=\"strong\">" + text + "</emphasis></speak>");
			break;
		case 2:
			String half1 = text.split(" ")[0];
			String[] rest = Arrays.copyOfRange(text.split(" "), 1, text.split(" ").length);
			speech.setSsml("<speak>" + half1 + "<break time=\"3s\"/>" + StringUtils.join(rest, " ") + "</speak>");
			break;
		case 3:
			String firstNoun = "erstes Wort buchstabiert";
			String firstN = text.split(" ")[3];
			speech.setSsml(
					"<speak>" + firstNoun + "<say-as interpret-as=\"spell-out\">" + firstN + "</say-as>" + "</speak>");
			break;
		case 4:
			speech.setSsml(
					"<speak><audio src='soundbank://soundlibrary/transportation/amzn_sfx_airplane_takeoff_whoosh_01'/></speak>");
			break;
		default:
			speech.setSsml("<speak><amazon:effect name=\"whispered\">" + text + "</amazon:effect></speak>");
		}

		return SpeechletResponse.newTellResponse(speech);
	}

	@Override
	public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
		logger.info("Alexa session ends now");
	}

	/*
	 * The first question presented to the skill user (entry point)
	 */
	private SpeechletResponse getWelcomeResponse() {
		String s = "Hallo!";
		SsmlOutputSpeech speech = new SsmlOutputSpeech();
		speech.setSsml("<speak>" + s + "</speak>");

		return askUserResponse("Hallo! Worauf hast du heute hunger?");
	}

	/**
	 * Tell the user something - the Alexa session ends after a 'tell'
	 */
	private SpeechletResponse continueConversation(String text) {
		// Create the plain text output.
		
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(text);
		
		PlainTextOutputSpeech speechOut = new PlainTextOutputSpeech();
		speechOut.setText("hey");
		
		Reprompt speech2 = new Reprompt();
		speech2.setOutputSpeech(speechOut);
		
		return SpeechletResponse.newAskResponse(speech, speech2);
	}
	
	private SpeechletResponse response(String text) {
		// Create the plain text output.
		
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(text);
		
		return SpeechletResponse.newTellResponse(speech);
	}


	/**
	 * A response to the original input - the session stays alive after an ask
	 * request was send. have a look on
	 * https://developer.amazon.com/de/docs/custom-skills/speech-synthesis-markup-language-ssml-reference.html
	 * 
	 * @param text
	 * @return
	 */
	private SpeechletResponse askUserResponse(String text) {
		SsmlOutputSpeech speech = new SsmlOutputSpeech();
		speech.setSsml("<speak>" + text + "</speak>");

		// reprompt after 8 seconds
		SsmlOutputSpeech repromptSpeech = new SsmlOutputSpeech();
		repromptSpeech.setSsml("<speak><emphasis level=\"strong\">Hey!</emphasis> Bist du noch da?</speak>");

		Reprompt rep = new Reprompt();
		rep.setOutputSpeech(repromptSpeech);

		return SpeechletResponse.newAskResponse(speech, rep);
	}

}
