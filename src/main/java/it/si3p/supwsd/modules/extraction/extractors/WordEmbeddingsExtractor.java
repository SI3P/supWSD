package it.si3p.supwsd.modules.extraction.extractors;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import it.si3p.supwsd.data.Annotation;
import it.si3p.supwsd.data.Lexel;
import it.si3p.supwsd.data.Token;
import it.si3p.supwsd.modules.extraction.extractors.we.WEMemoryManagment;
import it.si3p.supwsd.modules.extraction.extractors.we.strategy.WEIntegrationStrategy;
import it.si3p.supwsd.modules.extraction.extractors.we.strategy.WEStrategy;
import it.si3p.supwsd.modules.extraction.extractors.we.strategy.WEStrategyInstance;
import it.si3p.supwsd.modules.extraction.features.Feature;
import it.si3p.supwsd.modules.extraction.features.WordEmbedding;

/**
 * @author papandrea
 *
 */
public class WordEmbeddingsExtractor extends FeatureExtractor {

	private static final String DEFAULT_SEPARATOR = " ";
	private final WEIntegrationStrategy mStrategy;
	private final String mVocabFile;
	private final float mCacheSize;
	private final int mWindowSize;
	private final WEMemoryManagment mWEMemoryManagment;
	private final boolean lowercase;

	public WordEmbeddingsExtractor(WEStrategy strategy, int windowSize, String vectorsFile, String vocabFile,
			float cacheSize, boolean lowercase)  {

		super(0);

		this.mStrategy = WEStrategyInstance.getInstance().getIntegrationStrategy(strategy, windowSize);
		this.mWindowSize = windowSize;
		this.mVocabFile = vocabFile;
		this.mCacheSize = cacheSize;
		this.mWEMemoryManagment = new WEMemoryManagment(vectorsFile,DEFAULT_SEPARATOR);
		this.lowercase = lowercase;
	}

	@Override
	public Collection<Feature> extract(Lexel lexel, Annotation annotation) {

		Vector<Feature> features;
		Map<Integer, double[]> wordEmbeddings;
		Token[] tokens;
		String word;
		double value, wordEmbedding[];
		int id, min = 0, max,offset;
			
		id = lexel.getTokenIndex();
		offset=lexel.getOffset();
		tokens = annotation.getTokens(lexel);
		max = tokens.length - 1;

		if (mWindowSize > -1) {

			min = Math.max(min,id - this.mWindowSize);
			max = Math.min(max, id + this.mWindowSize);
		}

		max-=offset;
		features = new Vector<Feature>();
		wordEmbeddings = new HashMap<Integer, double[]>();

		for (int k = min; k <= max; k++) {
	
			word = tokens[k>id?k+offset:k].getWord();
			if(lowercase) {
				word = word.toLowerCase();
			}
			wordEmbedding = this.mWEMemoryManagment.get(word);
			wordEmbeddings.put(k - id, wordEmbedding);
		}

		for (int i = 0; i < this.mWEMemoryManagment.getMemSize(); i++) {

			value = 0;

			for (Entry<Integer, double[]> entry : wordEmbeddings.entrySet())
				value += mStrategy.coefficent(entry.getKey()) * entry.getValue()[i];

			features.add(new WordEmbedding(i, value));
		}

		wordEmbeddings.clear();

		return features;
	}

	@Override
	public void load() throws IOException {

		this.mWEMemoryManagment.load(mVocabFile, mCacheSize);
	}

	@Override
	public void unload() {

		this.mWEMemoryManagment.close();
	}

	@Override
	public Class<? extends Feature> getFeatureClass() {

		return WordEmbedding.class;
	}

}
