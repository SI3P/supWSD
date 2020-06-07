package it.si3p.supwsd.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import it.si3p.supwsd.inventory.SenseInventoryType;
import it.si3p.supwsd.modules.classification.classifiers.ClassifierType;
import it.si3p.supwsd.modules.extraction.extractors.SyntacticRelationsExtractor;
import it.si3p.supwsd.modules.extraction.extractors.LocalCollocationsExtractor;
import it.si3p.supwsd.modules.extraction.extractors.POSTagsExtractor;
import it.si3p.supwsd.modules.extraction.extractors.SurroundingWordsExtractor;
import it.si3p.supwsd.modules.extraction.extractors.WordEmbeddingsExtractor;
import it.si3p.supwsd.modules.extraction.extractors.we.strategy.WEStrategy;
import it.si3p.supwsd.modules.parser.ParserType;
import it.si3p.supwsd.modules.parser.xml.XMLHandler;
import it.si3p.supwsd.modules.preprocessing.units.tokenizer.TokenizerType;
import it.si3p.supwsd.modules.writer.WriterType;
import it.si3p.supwsd.modules.preprocessing.pipeline.PipelineType;
import it.si3p.supwsd.modules.preprocessing.units.dependencyParser.DependencyParserType;
import it.si3p.supwsd.modules.preprocessing.units.lemmatizer.LemmatizerType;
import it.si3p.supwsd.modules.preprocessing.units.splitter.SplitterType;
import it.si3p.supwsd.modules.preprocessing.units.tagger.TaggerType;

/**
 * @author papandrea
 *
 */
public class ConfigHandler extends XMLHandler {

	static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	private static final String NONE = "none";
	private final Config mConfig;
	private final Map<ConfigAttribute, String> mParams;
	private String mValue, mTag;

	public ConfigHandler(Config config) {

		this.mConfig = config;
		this.mParams = new HashMap<ConfigAttribute, String>();
	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {

		this.push(getTag(name));
		this.mValue = "";
		this.mParams.clear();

		for (int i = 0; i < attributes.getLength(); i++) {

			try {
				this.mParams.put(ConfigAttribute.valueOf(attributes.getLocalName(i).toUpperCase()),
						attributes.getValue(i));
				
			} catch (IllegalArgumentException e) {
				//throw new SAXException(e);
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {

		ConfigTag tag;

		tag = getTag(name);
		mTag = name;

		switch (tag) {

		case WORKING_DIRECTORY:

			this.mConfig.setWorkingDir(mValue);
			break;

		case PARSER:

			this.mConfig.setParserType(getValue(ParserType.class));
			this.mConfig.setMNS((String) getParam(String.class, ConfigAttribute.MNS, false));
			break;

		case WRITER:

			this.mConfig.setWriterType(getValue(WriterType.class));
			break;

		case CLASSIFIER:

			this.mConfig.setClassifierType(getValue(ClassifierType.class));
			break;

		case PIPELINE:

			this.mConfig.setPipeline(getValue(PipelineType.class),(String) getParam(String.class, ConfigAttribute.MODEL, true));
			break;
			
		case SPLITTER:

			this.mConfig.setSplitter(getValue(SplitterType.class),
					(String) getParam(String.class, ConfigAttribute.MODEL, false));
			break;

		case TOKENIZER:

			this.mConfig.setTokenizer(getValue(TokenizerType.class),
					(String) getParam(String.class, ConfigAttribute.MODEL, false));
			break;

		case TAGGER:

			this.mConfig.setTagger(getValue(TaggerType.class),
					(String) getParam(String.class, ConfigAttribute.MODEL, false));
			break;

		case LEMMATIZER:

			this.mConfig.setLemmatizer(getValue(LemmatizerType.class),
					(String) getParam(String.class, ConfigAttribute.MODEL, false));
			break;

		case DEPENDENCY_PARSER:

			this.mConfig.setDParser(getValue(DependencyParserType.class),
					(String) getParam(String.class, ConfigAttribute.MODEL, false));
			break;

		case POS_TAGS:

			if (getBooleanValue())
				this.mConfig.addFeatureExtractor(
						new POSTagsExtractor((int) getParam(Integer.class, ConfigAttribute.CUTOFF, 0)));
			break;

		case LOCAL_COLLOCATIONS:

			if (getBooleanValue())
				try {
					this.mConfig.addFeatureExtractor(
							new LocalCollocationsExtractor((int) getParam(Integer.class, ConfigAttribute.CUTOFF, 0),
									(String) getParam(String.class, ConfigAttribute.SEQUENCES, false)));
				} catch (IOException e) {
					throw new SAXException(e);
				}

			break;

		case SURROUNDING_WORDS:

			if (getBooleanValue())

				try {
					this.mConfig.addFeatureExtractor(
							new SurroundingWordsExtractor((int) getParam(Integer.class, ConfigAttribute.CUTOFF, 0),
									(int) getParam(Integer.class, ConfigAttribute.WINDOW, -1),
									(String) getParam(String.class, ConfigAttribute.STOPWORDS, false)));
				} catch (IOException e) {
					throw new SAXException(e);
				}

			break;

		case WORD_EMBEDDINGS:

			if (getBooleanValue())
				this.mConfig.addFeatureExtractor(
						new WordEmbeddingsExtractor(getEnumParam(WEStrategy.class, ConfigAttribute.STRATEGY),
								(int) getParam(Integer.class, ConfigAttribute.WINDOW, 4),
								(String) getParam(String.class, ConfigAttribute.VECTORS, true),
								(String) getParam(String.class, ConfigAttribute.VOCAB, false),
								(Float) getParam(Float.class, ConfigAttribute.CACHE, true),
								Boolean.parseBoolean((String)getParam(String.class, ConfigAttribute.LOWERCASE, false))));

			break;

		case SYNTACTIC_RELATIONS:

			if (getBooleanValue())
				this.mConfig.addFeatureExtractor(new SyntacticRelationsExtractor());
			break;

		case SENSE_INVENTORY:

			this.mConfig.setSenseInventory(getValue(SenseInventoryType.class),
					(String) getParam(String.class, ConfigAttribute.DICT, false),
					(String) getParam(String.class, ConfigAttribute.LANG, false));
			break;

		default:
			break;
		}

		this.pop();

	}

	@Override
	public void characters(char ch[], int start, int length) {

		mValue += new String(ch, start, length);

	}

	private ConfigTag getTag(String name) throws SAXException {

		try {

			return ConfigTag.valueOf(name.toUpperCase());

		} catch (IllegalArgumentException e) {

			throw new SAXException("Illegal xml tag <" + name + ">");
		}
	}

	private <T extends Enum<T>> T getValue(Class<T> cls) throws SAXException {

		try {

			if (mValue.equals(NONE))
				return null;
			else
				return Enum.valueOf(cls, mValue.toUpperCase());

		} catch (IllegalArgumentException e) {

			throw new SAXException("Illegal xml tag value " + mValue);
		}
	}

	private <T> Object getParam(Class<T> cls, ConfigAttribute name, boolean required) throws SAXException {

		Object value;
		String val;

		value = val = mParams.get(name);

		if (val != null && val.isEmpty())
			value = null;

		if (value != null) {

			try {

				if (cls.isAssignableFrom(Integer.class))
					value = Integer.valueOf(val);

				else if (cls.isAssignableFrom(Float.class))
					value = Float.valueOf(val);

			} catch (NumberFormatException e) {
				throw new SAXException(
						"Illegal value '" + val + "' for xml attribute " + name.name() + " of tag " + mTag);
			}

		} else if (required)
			throw new SAXException("Missed xml attribute '" + name.name() + "' for tag " + mTag);

		return value;
	}

	private <T> Object getParam(Class<T> cls, ConfigAttribute name, T def) throws SAXException {

		Object value;

		value = getParam(cls, name, false);

		if (value == null)
			value = def;

		return value;
	}

	private <T extends Enum<T>> T getEnumParam(Class<T> cls, ConfigAttribute name) throws SAXException {

		String val;

		val = mParams.get(name);

		if (val != null) {

			try {

				return Enum.valueOf(cls, val.toUpperCase());

			} catch (IllegalArgumentException e) {
				throw new SAXException("Illegal value '" + val + "' for xml attribute " + name.name());
			}
		} else
			throw new SAXException("Missed xml attribute '" + name.name() + "' for tag " + mTag);
	}

	private boolean getBooleanValue() {

		return Boolean.valueOf(mValue);
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		throw e;
	}
}
