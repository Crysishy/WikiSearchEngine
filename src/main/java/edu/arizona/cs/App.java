package edu.arizona.cs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.LMSimilarity;
import org.apache.lucene.search.similarities.MultiSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

/**
 * Programmer: YANG HONG
 * 
 */
public class App {

	private static int hitsPerPage = 1;
	private static final int hitsCategory = 10;
	// private static int countGlobal = 0;
	private static String answerGlobal = null;
	private static String[] wordsOfAnswer;
	private static File question100 = new File("./questions.txt");
	private static int detailedResult = 0, maxLength = 0;
	private static double totalNDCG = 0, averageNDCG = 0, totalCalculated = 0, benchmark = 0.5;
	// private static List<String> allDocs = new ArrayList<String>();

	public static void main(String[] args) throws IOException, ParseException {
		StandardAnalyzer analyzer = new StandardAnalyzer();
		Directory wikiDictionary = FSDirectory.open(Paths.get("./wikiIndexFiles"));
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter writer = new IndexWriter(wikiDictionary, config);

		//indexingWikiPages(writer);

		writer.close();

		print("Enter 1 or 2:\n1) 100 questions\n2) Manual input");
		Scanner keyboard = new Scanner(System.in);
		int choice = keyboard.nextInt();

		print("Enter 1 or 2:\n1) Simple result\n2) Detailed result");
		detailedResult = keyboard.nextInt();
		// printInt(detailedResult);
		print("newline");

		if (choice == 1)
			searching100Questions(analyzer, wikiDictionary);
		else
			searchingManualInput(analyzer, wikiDictionary);

		print("------------------------------");
		averageNDCG = totalNDCG / totalCalculated;

		print("Average NDCG: " + averageNDCG);
		print("------------------------------");
		print("Search completed.");
		// printInt(maxLength);

		keyboard.close();
	}

	private static void searchingManualInput(StandardAnalyzer analyzer, Directory wikiDictionary)
			throws ParseException, IOException {
		Scanner inputScanner = new Scanner(System.in);
		String choice = "y";
		String queryInput = null;

		while (choice.equals("y")) {
			answerGlobal = "not applicable";
			wordsOfAnswer = answerGlobal.split(" ");
			hitsPerPage = 200;

			print("Please enter a query:");
			queryInput = removeWeirdMarks(inputScanner.nextLine());
			print("newline");

			Query query = new QueryParser("body", analyzer).parse(queryInput);

			print("Multi-Similarity:");
			Similarity[] sims = { new BM25Similarity(), new ClassicSimilarity(), new LMDirichletSimilarity((float) 0.5),
					new LMJelinekMercerSimilarity((float) 0.5) };
			processQueryWithSimilarity(new MultiSimilarity(sims), wikiDictionary, query);

			System.out.print("Try another query? (y/n) ");
			choice = inputScanner.nextLine().toLowerCase();
		}

		inputScanner.close();
	}

	private static Document[] processCategoryWithSimilarity(Similarity multiSimilarity, Directory wikiDictionary,
			Query categoryQuery) throws IOException {
		IndexReader reader = DirectoryReader.open(wikiDictionary);
		IndexSearcher searcher = new IndexSearcher(reader);
		// Sort sort = new Sort(SortField.FIELD_SCORE);
		searcher.setSimilarity(multiSimilarity);
		TopDocs docs = searcher.search(categoryQuery, hitsCategory);
		ScoreDoc[] hits = docs.scoreDocs;

		Document[] doc1000 = new Document[hits.length];
		for (int n = 0; n < hits.length; n++) {
			int docId = hits[n].doc;
			doc1000[n] = searcher.doc(docId);
		}

		return doc1000;
	}

	private static void searching100Questions(StandardAnalyzer analyzer, Directory wikiDictionary)
			throws ParseException, IOException {
		// File question100 = new File("./questions.txt");
		boolean found = false;
		int countFound = 0;
		int count = 0;

		Scanner questionScanner = new Scanner(question100);
		while (questionScanner.hasNextLine()) {
			found = false;
			count = 0;

			questionScanner.nextLine();
			String question = questionScanner.nextLine();
			print("Clue: " + question);
			String answer = questionScanner.nextLine();
			String category = removeWeirdMarks(answer);

			// print(category);
			Query categoryQuery = new QueryParser("body", analyzer).parse(category);

			Similarity[] sims = { new BM25Similarity(), new ClassicSimilarity(), new LMDirichletSimilarity((float) 0.5),
					new LMJelinekMercerSimilarity((float) 0.5) };
			Document[] Top1000 = processCategoryWithSimilarity(new MultiSimilarity(sims), wikiDictionary,
					categoryQuery);
			// print("" + Top1000.length);

			Directory directory1000 = new RAMDirectory();
			IndexWriterConfig config1000 = new IndexWriterConfig(analyzer);
			IndexWriter writer1000 = new IndexWriter(directory1000, config1000);

			for (Document doc : Top1000) {
				writer1000.addDocument(doc);
				count++;
			}
			// printInt(count);

			writer1000.close();

			answerGlobal = removeWeirdMarks(answer);
			wordsOfAnswer = answerGlobal.split(" ");
			wordsOfAnswer = processWordsOfAnswer(wordsOfAnswer);

			question += " " + answer;
			question = removeWeirdMarks(question);

			Query query = new QueryParser("body", analyzer).parse(question);

			print("BM25 Similarity:");
			if (processQueryWithSimilarity(new BM25Similarity(), directory1000, query))
				found = true;

			print("tf-idf Similarity:");
			if (processQueryWithSimilarity(new ClassicSimilarity(), directory1000, query))
				found = true;

			print("LM Similarity with Mercer Smoothing:");
			if (processQueryWithSimilarity(new LMJelinekMercerSimilarity((float) 0.5), directory1000, query))
				found = true;

			print("LM Similarity with Dirichlet Smoothing:");
			if (processQueryWithSimilarity(new LMDirichletSimilarity((float) 0.5), directory1000, query))
				found = true;

			print("Multi-Similarity:");
			if (processQueryWithSimilarity(new MultiSimilarity(sims), directory1000, query))
				found = true;

			print("Answer: " + answer);
			print("Answer found: " + found);
			// print(answerGlobal);
			print("newline");
			print("------------------------------");
			questionScanner.nextLine();

			if (found)
				countFound++;
		}

		print("Result: " + countFound + " out of 100.");
		questionScanner.close();
	}

	private static String[] processWordsOfAnswer(String[] words) {
		for (int i = 0; i < words.length; i++) {
			words[i] = words[i].replace("(", "");
			words[i] = words[i].replace(")", "");
			words[i] = words[i].replace(",", "");
			words[i] = words[i].replace("-", " ");
		}

		return words;
	}

	private static String removeWeirdMarks(String str) {
		return str.replace("!", " ").replace("-", " ").replace("|", " ");
	}

	@SuppressWarnings("unused")
	private static void indexingWikiPages(IndexWriter writer) throws IOException {
		print("Indexing wiki pages...");
		String fileIndex = null, currentTitle = null, futureTitle = null, body = "", currentLine = null;
		File input = null;
		Scanner scanner = null;
		int count = 1;

		// ATTENTION!
		for (int i = 0; i < 1300; i++) {
			currentTitle = "";
			// ATTENTION!
			fileIndex = String.format("%04d", i);
			input = new File("./wiki-subset-20140602/enwiki-20140602-pages-articles.xml-" + fileIndex + ".txt");

			// ATTENTION!
			if (!input.exists())
				continue;

			scanner = new Scanner(input);
			// BBS
			currentTitle = scanner.nextLine();
			if (currentTitle.equals(""))
				currentTitle = scanner.nextLine();
			currentTitle = trimTitle(currentTitle);

			// print(currentTitle);

			while (scanner.hasNextLine()) {
				currentLine = scanner.nextLine();
				if (currentLine.equals("") || currentLine.length() < 2)
					continue;

				if (currentLine.charAt(0) == '[' && currentLine.charAt(1) == '[' && currentLine.contains("]]")) {
					futureTitle = trimTitle(currentLine);
					// print("Title: " + currentTitle);
					// print("Body : " + body);

					addDoc(writer, currentTitle.trim(), body.trim());
					if (count % 1000 == 0)
						printInt(count);

					count++;

					currentTitle = futureTitle;
					body = "";
				} else
					body += currentLine;
			}

			// print(futureTitle);
			addDoc(writer, futureTitle.trim(), body.trim());
			print("Total of " + count + " documents indexed.");

			// ATTENTION!
		}
	}

	private static String trimTitle(String string) {
		String str = string.substring(string.indexOf("[[") + 2, string.indexOf("]]"));
		return str;
	}

	private static boolean processQueryWithSimilarity(Similarity similarity, Directory index, Query query)
			throws IOException {
		boolean found = false;
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		// Sort sort = new Sort(SortField.FIELD_SCORE);
		searcher.setSimilarity(similarity);

		if (detailedResult == 2)
			hitsPerPage = 5;

		TopDocs docs = searcher.search(query, hitsPerPage);
		ScoreDoc[] hits = docs.scoreDocs;

		// List<String> tempDocs = new ArrayList<String>();
		// for (String str : allDocs)
		// tempDocs.add(str);

		double topScoring = hits[0].score;
		double DCG = 0, IDCG = 0, NDCG = 0;
		for (int n = 0; n < hits.length; n++) {
			double relevance = 0;
			double thisDCG = 0;

			if (hits[n].score / topScoring > benchmark)
				relevance = 1;

			thisDCG = ((Math.pow(2, relevance) - 1) / (Math.log(n + 2) / Math.log(2)));
			DCG += thisDCG;
			IDCG += ((Math.pow(2, 1) - 1) / (Math.log(n + 2) / Math.log(2)));

			int docId = hits[n].doc;
			Document d = searcher.doc(docId);

			// get words from the first returned result
			// not affected under change of hitsPerPage
			String[] words = searcher.doc(hits[0].doc).get("Title").split(" ");
			words = processWordsOfAnswer(words);

			for (String word1 : words) {
				word1 = word1.toLowerCase();
				for (String word2 : wordsOfAnswer) {
					word2 = word2.toLowerCase();
					if (word1.equals(word2))
						found = true;
				}
			}
			// tempDocs.remove(String.valueOf(docId));

			if (detailedResult == 2)
				System.out.printf("%-50s \t %f\n", d.get("Title"), hits[n].score);
			// print(d.get("Title") + "\t" + hits[n].score);
			else
				print(d.get("Title"));
		}

		NDCG = DCG / IDCG;
		totalNDCG += NDCG;
		totalCalculated++;

		if (detailedResult == 2) {
			print(" DCG=" + DCG);
			print("IDCG=" + IDCG);
			print("NDCG=" + NDCG);
		}
		// while (!tempDocs.isEmpty()){
		// Document d1 = searcher.doc(Integer.parseInt(tempDocs.get(0)));
		// print(d1.get("docID") + "\t" + "0");
		// tempDocs.remove(0);
		// }
		reader.close();
		// print("" + found);
		print("newline");
		return found;
	}

	private static void addDoc(IndexWriter writer, String currentTitle, String body) throws IOException {
		Document doc = new Document();
		doc.add(new StringField("Title", currentTitle, Field.Store.YES));
		doc.add(new TextField("body", body, Field.Store.YES));
		writer.addDocument(doc);
	}

	private static void print(String toBePrinted) {
		if (toBePrinted.equals("newline"))
			System.out.println();
		else
			System.out.println(toBePrinted);
	}

	private static void printInt(int num) {
		System.out.println(num);
	}
}
