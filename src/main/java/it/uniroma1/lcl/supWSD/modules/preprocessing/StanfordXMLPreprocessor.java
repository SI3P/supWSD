package it.uniroma1.lcl.supWSD.modules.preprocessing;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;
import it.uniroma1.lcl.supWSD.data.Annotation;
import it.uniroma1.lcl.supWSD.data.Lexel;
import it.uniroma1.lcl.supWSD.modules.preprocessing.units.dependencyParser.dependencyTree.DependencyRelation;
import it.uniroma1.lcl.supWSD.modules.preprocessing.units.dependencyParser.dependencyTree.DependencyTree;

/**
 * @author Simone Papandrea
 *
 */
public class StanfordXMLPreprocessor extends StanfordPreprocessor {

	private final ConcurrentHashMap<Integer, HashSet<Integer>> mIndex;

	public StanfordXMLPreprocessor(boolean split, boolean pos, boolean lemma, boolean depparse) {

		super(split, pos, lemma, depparse, true);

		mIndex = new ConcurrentHashMap<Integer, HashSet<Integer>>();
	}

	@Override
	String init(Annotation annotation) {

		Pattern pattern;
		Matcher matcher;
		int count = 0;
		HashSet<Integer> indexes;

		pattern = Pattern.compile("\\b"+Pattern.quote(Annotation.ANNOTATION_TAG));
		matcher = pattern.matcher(annotation.getText());
		indexes = new HashSet<Integer>();

		while (matcher.find()) {

			indexes.add(matcher.start() - count);
			count += Annotation.ANNOTATION_TAG.length() * 2;
			matcher.find();
		}

		mIndex.put(annotation.getID(), indexes);

		return matcher.replaceAll("");
	}

	@Override
	void annote(Annotation annotation, List<CoreMap> sentences) {

		List<CoreLabel> labels;
		CoreLabel label;
		CoreMap sentence;
		SemanticGraph graph;
		String tokens[][], tags[][], lemmas[][], words[];
		DependencyTree[] dtree;
		Iterator<Lexel> lexels;
		HashSet<Integer> indexes;
		int id, size, length;

		size = sentences.size();
		tokens = new String[size][];
		tags = new String[size][];
		lemmas = new String[size][];
		dtree = new DependencyTree[size];
		lexels = annotation.iterator();
		id = annotation.getID();
		indexes = mIndex.get(id);

		for (int i = 0; i < size; i++) {

			sentence = sentences.get(i);
			labels = sentence.get(TokensAnnotation.class);
			length = labels.size();
			words = new String[length];
			tags[i] = new String[length];
			lemmas[i] = new String[length];

			for (int j = 0; j < length; j++) {

				label = labels.get(j);
				words[j] = label.get(TextAnnotation.class);
				tags[i][j] = label.get(PartOfSpeechAnnotation.class);
				lemmas[i][j] = label.get(LemmaAnnotation.class);

				if (indexes.contains(label.beginPosition()))
					lexels.next().set(i, j);
			}

			tokens[i] = words;
			graph = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);

			if (graph != null)
				dtree[i] = getDependencyTree(graph);
		}

		
		for(Lexel lexel:annotation.getLexels())
			if(lexel.getSentenceIndex()==-1)
				System.out.println(annotation);
				
		annotation.annote(tokens, mPOS ? tags : null, mLemma ? lemmas : null, mDepParse ? dtree : null);
		mIndex.remove(id);
	}

	private DependencyTree getDependencyTree(SemanticGraph graph) {

		DependencyTree dtree = null;
		IndexedWord dep;

		dtree = new DependencyTree();

		for (IndexedWord vertex : graph.vertexSet()) {

			if (vertex.tag() != null) {

				for (SemanticGraphEdge edge : graph.getOutEdgesSorted(vertex)) {

					dep = edge.getDependent();
					dtree.add(vertex.index(), vertex.value(), vertex.tag(), dep.index(), dep.value(), dep.tag(),
							edge.getRelation().getShortName().equals("nsubjpass") ? DependencyRelation.PASSIVE
									: DependencyRelation.ACTIVE);
				}
			}
		}

		return dtree;
	}
}