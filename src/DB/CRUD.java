package DB;

import java.sql.SQLException;

public interface CRUD {
    String create(String[] values) ;

    String read();

    String update(String[] values);

    String delete(String primaryKey);
}
