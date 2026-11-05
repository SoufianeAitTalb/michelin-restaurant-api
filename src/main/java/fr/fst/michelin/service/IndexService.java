package fr.fst.michelin.service;

import fr.fst.michelin.entity.EvaluationEntity;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class IndexService {

    private static final int NOMBRE_RESULTATS = 100;

    private final IndexWriter indexWriter;
    private final QueryParser commentaireQueryParser;

    public IndexService(@Value("${lucene.index}") String indexDirectoryPath) throws IOException {
        Path path = Paths.get(indexDirectoryPath);
        Directory index = FSDirectory.open(path);

        StandardAnalyzer analyzer = new StandardAnalyzer();
        this.commentaireQueryParser = new QueryParser("commentaire", analyzer);

        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        this.indexWriter = new IndexWriter(index, indexWriterConfig);
    }

    public void indexEvaluation(EvaluationEntity evaluation) {
        try {
            Document document = new Document();
            document.add(new StringField("id", String.valueOf(evaluation.getId()), Field.Store.YES));
            document.add(new TextField("commentaire", evaluation.getCommentaire(), Field.Store.YES));

            this.indexWriter.updateDocument(new Term("id", String.valueOf(evaluation.getId())), document);
            this.indexWriter.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteEvaluation(Integer id) {
        try {
            this.indexWriter.deleteDocuments(new Term("id", String.valueOf(id)));
            this.indexWriter.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Integer> searchEvaluations(List<String> motsCles) {
        String texte = QueryParser.escape(String.join(" ", motsCles));
        if (texte.isBlank()) {
            return new ArrayList<>();
        }

        try (DirectoryReader directoryReader = DirectoryReader.open(this.indexWriter)) {
            IndexSearcher indexSearcher = new IndexSearcher(directoryReader);
            Query query = this.commentaireQueryParser.parse(texte);
            TopDocs topDocs = indexSearcher.search(query, NOMBRE_RESULTATS);

            List<Integer> ids = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                String id = indexSearcher.storedFields().document(scoreDoc.doc).get("id");
                ids.add(Integer.valueOf(id));
            }
            return ids;
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
