package bot.botGradle.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Words {

    @Id
    private Integer id;
    private String wordRu;
    private String wordEn;
    private String trans;
    private int countWordRu;
    private int countWordEn;

}
