package bot.botGradle.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Entity
public class User {

    @Id
    private Long chatId;
    private String firstName;
    private String userName;
    private String userChoose;
    private String userLanguage;
    private String userLastWord;
    private int userLastWordId;
    private Timestamp registeredAt;

}
