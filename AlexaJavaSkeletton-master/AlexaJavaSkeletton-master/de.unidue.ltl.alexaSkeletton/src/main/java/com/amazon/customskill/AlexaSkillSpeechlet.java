/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.amazon.customskill;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.customskill.exceptions.DeviceAddressClientException;
import com.amazon.customskill.exceptions.UnauthorizedException;
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

import nlp.dkpro.backend.MyJWKTL;
import nlp.dkpro.backend.NlpSingleton;
import nlp.dkpro.backend.PosTagger;

/*
 * This class is the actual skill. Here you receive the input and have to produce the speech output. 
 */
public class AlexaSkillSpeechlet implements SpeechletV2 {

    /**
     * The permissions that this skill relies on for retrieving addresses. If the consent token isn't
     * available or invalid, we will request the user to grant us the following permission
     * via a permission card.
     *
     * Another Possible value if you only want permissions for the country and postal code is:
     * read::alexa:device:all:address:country_and_postal_code
     * Be sure to check your permissions settings for your skill on https://developer.amazon.com/
     */
	
	private static final String ADDRESS_CARD_TITLE = "Ich darf deine Adresse nicht lesen";
    private static final String ALL_ADDRESS_PERMISSION = "read::alexa:device:all:address";
	public static String userRequest;
	public String address;
	String userSaid;
	
	public long distance = 1000;
	int listSize = 0;
	int index = 0;
	
	boolean rating = false;
	
	public static ArrayList<String> conversation = new ArrayList<String>();
	public static ArrayList<String> queryTokensN = new ArrayList<String>();
	public static ArrayList<String> queryTokensJ = new ArrayList<String>();
	ArrayList<Restaurant> foundRestaurants;
	ArrayList<String> list = new ArrayList<String>();
	
	private PosTagger posTagger;

	static Logger logger = LoggerFactory.getLogger(AlexaSkillSpeechlet.class);

	@Override
	public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
		posTagger = NlpSingleton.getInstance();
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
        		+ "Würdest du mir diese bitte geben? Ich habe dir die Erlaubnisanfrage an deine Amazon Alexa App geschickt.";

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
    
    private SpeechletResponse handleBuiltInIntents(Intent intent) {
    	String intentName = intent.getName();
    	switch(intentName) {
    	case "AMAZON.StopIntent":
    		//conversation.clear();
    		clean();
    		return response("Bis zum nächsten Mal!");
    	}
    	return null;
    }

    private void checkPermissionsResponse(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) throws UnauthorizedException {
		Session session = requestEnvelope.getSession();
		Permissions permissions = session.getUser().getPermissions();
        if (permissions == null) {
        	throw new UnauthorizedException("Es besteht keine Erlaubnis die Adressdaten zu benutzen");
        }
    }
    
    private String getUserAddress(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
    	try {
			SystemState systemState = getSystemState(requestEnvelope.getContext());
	        String apiAccessToken = systemState.getApiAccessToken();
	        String deviceId = systemState.getDevice().getDeviceId();
	        String apiEndpoint = systemState.getApiEndpoint();
	        AlexaDeviceAddressClient alexaDeviceAddressClient = new AlexaDeviceAddressClient(deviceId, apiAccessToken, apiEndpoint);
			Address addressObject = alexaDeviceAddressClient.getFullAddress();
			String address = addressObject.getCity() + ", " + addressObject.getAddressLine1() + " " + addressObject.getAddressLine2(); 
			return address;
		} catch (DeviceAddressClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return null;
    }
    
	@Override
	public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
		IntentRequest request = requestEnvelope.getRequest();	
		Intent intent = request.getIntent();
		SpeechletResponse builtInResponse = this.handleBuiltInIntents(intent); 
		if (builtInResponse != null) {
			return builtInResponse;
		}
		
		//um ohne Alexa Gerät testen zu können - das AUSKOMMENTIEREN
		try {
			checkPermissionsResponse(requestEnvelope);
		} catch (UnauthorizedException e) {
			return getPermissionsResponse();
		}
		this.address = getUserAddress(requestEnvelope);
		//bis hier
		//und eine Adresse unten hardkodieren
		//address = hier eine Adresse schreiben (z.b. "Duisburg, Hauptstrasse 1");
		
		Map<String, Slot> slots = intent.getSlots();
		
		if (slots.get("alles").getValue() != null) {
			//Debug
			System.out.println("Du hast gesagt: " + slots.get("alles").getValue());
			
			userRequest = intent.getSlot("alles").getValue();
			conversation.add(userRequest);
			
			if (conversation.size() == 1) {
				queryTokensN = analyze(userRequest, "N");
				queryTokensJ = analyze(userRequest, "ADJ");
				
				if (userRequest.contains("pizza")) {
					queryTokensN.add("pizza");
				}
			
				/*
				try {
					queryTokensN = addSynonyms(queryTokensN);
					queryTokensJ = addSynonyms(queryTokensJ);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (XMLStreamException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				*/
				
				if((queryTokensN.isEmpty() && queryTokensJ.isEmpty()) || queryTokensN.contains("ahnung") || queryTokensN.contains("keine") || queryTokensN.contains("irgendwas") || queryTokensN.contains("nicht")) {
					conversation.clear();
					return continueConversation(Constants.noMatches);
				}
				
				//Debug
				System.out.println("Nomens:");
				if(queryTokensN.size() != 0) {
					for (int i = 0; i < queryTokensN.size(); i++) {
						System.out.println(queryTokensN.get(i));
					}
				}
				
				System.out.println("ADJ:");
				if(queryTokensJ.size() != 0) {
					for (int i = 0; i < queryTokensJ.size(); i++) {
						System.out.println(queryTokensJ.get(i));
					}
				}
				
				return continueConversation(Constants.distance);
				
			} else if(conversation.size() == 2) {		
				NumberWordsToNumbers wandler = new NumberWordsToNumbers();
				try {
					String s = StringFilter.FilterForDistance(userRequest);
					distance = wandler.Umwandeln(s);
					//Debug
					System.out.println("dist = " + distance);
					
					if (distance == 0) {
						conversation.remove(1);
						return(continueConversation(Constants.invalidDistance));
					}
					
					if (distance > 5000) {
						conversation.remove(1);
						return(continueConversation(Constants.bigDistance));
					}
					
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					conversation.remove(1);
					return(continueConversation(Constants.invalidDistance));
				}
				
				return(continueConversation(Constants.rating));

			} else if(conversation.size() == 3) {
				
				try {
					ArrayList<Restaurant> restaurants = RestaurantFinder.getData(address, distance);					
					ArrayList<String> foundFood = findMatch(queryTokensN, restaurants, this::constructFoundFood);
					ArrayList<String> foundKitchen = new ArrayList<String>();
					if(foundFood.isEmpty()) {
						foundKitchen = findMatch(queryTokensJ, restaurants, this::constructFoundKitchen);
					}
					
					String answer = ""; 
					if (!foundFood.isEmpty() || !foundKitchen.isEmpty()) {
						if (listSize == 1) {
							answer = "Ich habe für dich ein Restaurant in der nähe gefunden. Das ist ";
							conversation.clear();
							
						} else {
							answer = "Ich habe für dich " + listSize + " Restaurants in der nähe gefunden. Das erste Restaurant ist ";
							index++;
						}
						answer += foundFood.isEmpty() ? foundKitchen.isEmpty() ? " " : foundKitchen.get(0) : foundFood.get(0);
						if (userRequest.contains("ja")) {
							rating = true;
							foundRestaurants = ListUtilities.sortListByRating(foundRestaurants);
							answer += "Dieses Restaurant ist mit " + foundRestaurants.get(0).getRating() + " bewertet. ";
							System.out.println("Answer: " + answer);
						}
						if (listSize > 1) {
							answer += "Soll ich das nächste Restaurant nennen?";
							return continueConversation(answer);
						} else {
							answer += getFakt();
						}
						clean();
						return response(answer);
					} else {
						conversation.clear();
						return continueConversation(Constants.nix);
					}	
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			} else if (conversation.size() >= 4) {
				if (userRequest.contains("ja")) {
					String answer = "Das nächste Restaurant ist ";
					answer += list.get(index);
					if (rating == true) {
						answer += "Dieses Restaurant ist mit " + foundRestaurants.get(index).getRating() + " bewertet. ";
					}
					index++;
					if (index == listSize) {
						answer += " Das war das letzte Restaurant, das ich gefunden habe. ";
						answer += getFakt();
						clean();
						return response(answer);
					} else {
						int rest = listSize - index;
						String r = "";
						System.out.println("Rest: " + rest);
						if (rest == 1) {
							r = "ein Restaurant ";
						} else {
							r = "" + rest + " Restaurants ";
						}
						answer += "Ich habe noch " + r + "für dich. Soll ich das nächste Restaurant nennen?"; 
						return continueConversation(answer);
					}
				} else {
					clean();
					return response("Okay. " + getFakt());
				}
			}
		} else {
			return continueConversation(Constants.nothing);
		}
		return continueConversation(Constants.nix);	
	}
	
	private String getFakt() {
		ArrayList<String> fakts = FactProvider.getFactProvider().getFact(userSaid);
		Random r = new Random();
		String a = "";
		if (fakts.size() == 0) {
			fakts = FactProvider.getFactProvider().getFact("default");
			String fakt = fakts.get(r.nextInt(((fakts.size()-1) - 0) + 1) + 0);
			a = "Hier ist noch ein interessanter Fakt. " + fakt;
		} else {
			String fakt = fakts.get(r.nextInt(((fakts.size()-1) - 0) + 1) + 0);
			a = "Hier ist noch ein interessanter Fakt über das ausgewählte Essen. " + fakt;
		}	
		return a;
	}
	
	private void clean() {
		index = 0;
		listSize = 0;
		list.clear();
		conversation.clear();
		rating = false;
		foundRestaurants.clear();
	}
	
	private ArrayList<String> findMatch(ArrayList<String> queryTokens, ArrayList<Restaurant> restaurants, BiFunction<String, String, String> constructFound) {
		String result = "";
		HashMap<Restaurant, String> found = queryRestaurants(restaurants, queryTokens);
		foundRestaurants = new ArrayList<>(found.keySet());
		foundRestaurants = ListUtilities.sortListByDistance(foundRestaurants);
		for(Restaurant i : foundRestaurants) {
			result = found.size() > 0 ? constructFound.apply(found.get(i), i.getName()) : Constants.nix;
			list.add(result);
		}
		listSize = list.size();
		System.out.println("FoundRestaurants: " + foundRestaurants.size());
		return list;
	}
	
	private String constructFoundFood(String what, String restaurant) {
		userSaid = what;
		StringBuilder sb = new StringBuilder();
		sb.append("das Restaurant ");
		sb.append(restaurant);
		sb.append(", wo du ");
		sb.append(what);
		sb.append(" essen kannst. ");
		System.out.println("Alexa antwortet: " + sb.toString());
		return sb.toString();
	}
	
	private String constructFoundKitchen (String what, String restaurant) {
		userSaid = what;
		StringBuilder sb = new StringBuilder();
		sb.append("das Restaurant ");
		sb.append(restaurant);
		sb.append(", wo du ");
		sb.append(what);
		sb.append(" Küche essen kannst. ");
		System.out.println("Alexa antwortet: " + sb.toString());
		return sb.toString();
	}
	
	private HashMap<Restaurant, String> queryRestaurants(ArrayList<Restaurant> restaurants, ArrayList<String> queryTokens) {
		HashMap<Restaurant, String> result = new HashMap<Restaurant, String>();
		for(Restaurant restaurant: restaurants) {
			for(String token: queryTokens) {
				if(restaurant.getTitle().contains(token) || restaurant.getAlias().contains(token) || restaurant.getTitle().contains(token.substring(0, token.length()-1))) {
					result.put(restaurant, token);
				}
			}
		}
		return result;
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
		speech.setSsml("<speak><emphasis level=\"strong\">" + s + "</emphasis> Bist du noch da?</speak>");

		return askUserResponse(s + "Welches Gericht oder welche Küche möchtest du gerne essen?");
	}

	/**
	 * Tell the user something - the Alexa session ends after a 'tell'
	 */
	private SpeechletResponse continueConversation(String text) {
		// Create the plain text output.
		
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(text);
		
		PlainTextOutputSpeech speechOut = new PlainTextOutputSpeech();
		
		speechOut.setText("Hey! Bist du noch da?");
		
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
		repromptSpeech.setSsml("<speak><amazon:effect name=\"whispered\">" + "Hey! Bist du noch da?" + "</amazon:effect></speak>");

		Reprompt rep = new Reprompt();
		rep.setOutputSpeech(repromptSpeech);

		return SpeechletResponse.newAskResponse(speech, rep);
	}
	
	private ArrayList<String> analyze(String request, String tag) {
        ArrayList<String> food = new ArrayList<>();
        try {
        	food = posTagger.findByTag(userRequest, tag);   
        }
        catch (Exception e) {
            throw new UnsupportedOperationException();
        }
        return food;
    }
	
	private ArrayList<String> addSynonyms(ArrayList<String> list) throws FileNotFoundException, XMLStreamException, IOException {
		ArrayList<String> result = new ArrayList<String>();
		result.addAll(list);
		
		for (String word : list) {
			//debug
			System.out.println("Word: " + word);
			ArrayList<String> syn = MyJWKTL.getSynonym(word);
			//debug
			System.out.println("Syn.size " + syn.size());
			for (int i = 0; i < syn.size(); i++) {
				System.out.println(syn.get(i));
			}
			result.addAll(syn);
		}
		
		return result;
	}
}