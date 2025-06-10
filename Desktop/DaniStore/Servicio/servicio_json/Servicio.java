/*
  Servicio.java
  Servicio web tipo REST
  Recibe parámetros utilizando JSON
  Carlos Pineda Guerrero, septiembre 2024
*/

package servicio_json;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.Response;

import java.sql.*;
import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import com.google.gson.*;

// la URL del servicio web es http://localhost:8080/Servicio/rest/ws
// donde:
//	"Servicio" es el dominio del servicio web (es decir, el nombre de archivo Servicio.war)
//	"rest" se define en la etiqueta <url-pattern> de <servlet-mapping> en el archivo WEB-INF\web.xml
//	"ws" se define en la siguiente anotación @Path de la clase Servicio

@Path("ws")
public class Servicio
{
  static DataSource pool = null;
  static
  {		
    try
    {
      Context ctx = new InitialContext();
      pool = (DataSource)ctx.lookup("java:comp/env/jdbc/datasource_Servicio");
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  static Gson j = new GsonBuilder().registerTypeAdapter(byte[].class,new AdaptadorGsonBase64()).setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").create();

  @POST
  @Path("alta_usuario")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response alta(String json) throws Exception
  {
    ParamAltaUsuario p = (ParamAltaUsuario) j.fromJson(json,ParamAltaUsuario.class);
    Usuario usuario = p.usuario;

    Connection conexion = pool.getConnection();

    if (usuario.email == null || usuario.email.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el email"))).build();

    if (usuario.nombre == null || usuario.nombre.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el nombre"))).build();

    if (usuario.apellido_paterno == null || usuario.apellido_paterno.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el apellido paterno"))).build();

    if (usuario.fecha_nacimiento == null)
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar la fecha de nacimiento"))).build();

    try
    {
      conexion.setAutoCommit(false);

      PreparedStatement stmt_1 = conexion.prepareStatement("INSERT INTO usuarios(id_usuario,email,nombre,apellido_paterno,apellido_materno,fecha_nacimiento,telefono,genero,password,token) VALUES (0,?,?,?,?,?,?,?,?,?)");
 
      try
      {
        stmt_1.setString(1,usuario.email);
        stmt_1.setString(2,usuario.nombre);
        stmt_1.setString(3,usuario.apellido_paterno);

        if (usuario.apellido_materno != null)
          stmt_1.setString(4,usuario.apellido_materno);
        else
          stmt_1.setNull(4,Types.VARCHAR);

        stmt_1.setTimestamp(5,usuario.fecha_nacimiento);

        if (usuario.telefono != null)
          stmt_1.setLong(6,usuario.telefono);
        else
          stmt_1.setNull(6,Types.BIGINT);

        if (usuario.genero != null)
          stmt_1.setString(7,usuario.genero);
        else
          stmt_1.setNull(7,Types.CHAR);
        stmt_1.setString(8, usuario.password != null ? usuario.password : "");
        stmt_1.setString(9, usuario.token != null ? usuario.token : "");

        stmt_1.executeUpdate();
      }
      finally
      {
        stmt_1.close();
      }

      if (usuario.foto != null)
      {
        PreparedStatement stmt_2 = conexion.prepareStatement("INSERT INTO fotos_usuarios(id_foto,foto,id_usuario) VALUES (0,?,LAST_INSERT_ID())");
        try
        {
          stmt_2.setBytes(1,usuario.foto);
          stmt_2.executeUpdate();
        }
        finally
        {
          stmt_2.close();
        }
      }
      conexion.commit();
    }
    catch (Exception e)
    {
      conexion.rollback();
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
    finally
    {
      conexion.setAutoCommit(true);
      conexion.close();
    }
    return Response.ok().build();
  }

  @POST
  @Path("consulta_usuario")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response consulta(String json) throws Exception
  {
    ParamConsultaUsuario p = (ParamConsultaUsuario) j.fromJson(json,ParamConsultaUsuario.class);
    String email = p.email;

    Connection conexion= pool.getConnection();

    try
    {
      PreparedStatement stmt_1 = conexion.prepareStatement("SELECT a.email,a.nombre,a.apellido_paterno,a.apellido_materno,a.fecha_nacimiento,a.telefono,a.genero,b.foto FROM usuarios a LEFT OUTER JOIN fotos_usuarios b ON a.id_usuario=b.id_usuario WHERE email=?");
      try
      {
        stmt_1.setString(1,email);

        ResultSet rs = stmt_1.executeQuery();
        try
        {
          if (rs.next())
          {
            Usuario r = new Usuario();
            r.email = rs.getString(1);
            r.nombre = rs.getString(2);
            r.apellido_paterno = rs.getString(3);
            r.apellido_materno = rs.getString(4);
            r.fecha_nacimiento = rs.getTimestamp(5);
            r.telefono = rs.getObject(6) != null ? rs.getLong(6) : null;
            r.genero = rs.getString(7);
	    r.foto = rs.getBytes(8);
            return Response.ok().entity(j.toJson(r)).build();
          }
          return Response.status(400).entity(j.toJson(new Error("El email no existe"))).build();
        }
        finally
        {
          rs.close();
        }
      }
      finally
      {
        stmt_1.close();
      }
    }
    catch (Exception e)
    {
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
    finally
    {
      conexion.close();
    }
  }

  @POST
  @Path("modifica_usuario")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response modifica(String json) throws Exception
  {
    ParamModificaUsuario p = (ParamModificaUsuario) j.fromJson(json,ParamModificaUsuario.class);
    Usuario usuario = p.usuario;

    Connection conexion= pool.getConnection();

    if (usuario.email == null || usuario.email.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el email"))).build();

    if (usuario.nombre == null || usuario.nombre.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el nombre"))).build();

    if (usuario.apellido_paterno == null || usuario.apellido_paterno.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el apellido paterno"))).build();

    if (usuario.fecha_nacimiento == null)
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar la fecha de nacimiento"))).build();

    conexion.setAutoCommit(false);
    try
    {
      // Construir el SQL dinámicamente para actualizar password y token solo si se reciben
      StringBuilder sql = new StringBuilder("UPDATE usuarios SET nombre=?,apellido_paterno=?,apellido_materno=?,fecha_nacimiento=?,telefono=?,genero=?");
      java.util.List<Object> params = new java.util.ArrayList<>();
      params.add(usuario.nombre);
      params.add(usuario.apellido_paterno);
      params.add(usuario.apellido_materno != null ? usuario.apellido_materno : null);
      params.add(usuario.fecha_nacimiento);
      params.add(usuario.telefono != null ? usuario.telefono : null);
      params.add(usuario.genero != null ? usuario.genero : null);
      if (usuario.password != null && !usuario.password.isEmpty()) {
        sql.append(",password=?");
        params.add(usuario.password);
      }
      if (usuario.token != null && !usuario.token.isEmpty()) {
        sql.append(",token=?");
        params.add(usuario.token);
      }
      sql.append(" WHERE email=?");
      params.add(usuario.email);

      PreparedStatement stmt_1 = conexion.prepareStatement(sql.toString());
      try
      {
        for (int i = 0; i < params.size(); i++) {
          Object param = params.get(i);
          if (param == null) {
            // Determinar tipo según el índice
            if (i == 2) stmt_1.setNull(i+1, Types.VARCHAR); // apellido_materno
            else if (i == 4) stmt_1.setNull(i+1, Types.BIGINT); // telefono
            else if (i == 5) stmt_1.setNull(i+1, Types.CHAR); // genero
            else stmt_1.setNull(i+1, Types.VARCHAR);
          } else {
            stmt_1.setObject(i+1, param);
          }
        }
        stmt_1.executeUpdate();
      }
      finally
      {
        stmt_1.close();
      }

      PreparedStatement stmt_2 = conexion.prepareStatement("DELETE FROM fotos_usuarios WHERE id_usuario=(SELECT id_usuario FROM usuarios WHERE email=?)");
      try
      {
        stmt_2.setString(1,usuario.email);
        stmt_2.executeUpdate();
      }
      finally
      {
        stmt_2.close();
      }

      if (usuario.foto != null)
      {
        PreparedStatement stmt_3 = conexion.prepareStatement("INSERT INTO fotos_usuarios(id_foto,foto,id_usuario) VALUES (0,?,(SELECT id_usuario FROM usuarios WHERE email=?))");
        try
        {
          stmt_3.setBytes(1,usuario.foto);
          stmt_3.setString(2,usuario.email);
          stmt_3.executeUpdate();
        }
        finally
        {
          stmt_3.close();
        }
      }
      conexion.commit();
    }
    catch (Exception e)
    {
      conexion.rollback();
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
    finally
    {
      conexion.setAutoCommit(true);
      conexion.close();
    }
    return Response.ok().build();
  }

  @POST
  @Path("borra_usuario")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response borra(String json) throws Exception
  {
    ParamBorraUsuario p = (ParamBorraUsuario) j.fromJson(json,ParamBorraUsuario.class);
    String email = p.email;

    Connection conexion= pool.getConnection();

    try
    {
      PreparedStatement stmt_1 = conexion.prepareStatement("SELECT 1 FROM usuarios WHERE email=?");
      try
      {
        stmt_1.setString(1,email);

        ResultSet rs = stmt_1.executeQuery();
        try
        {
          if (!rs.next())
		return Response.status(400).entity(j.toJson(new Error("El email no existe"))).build();
        }
        finally
        {
          rs.close();
        }
      }
      finally
      {
        stmt_1.close();
      }
      conexion.setAutoCommit(false);
      PreparedStatement stmt_2 = conexion.prepareStatement("DELETE FROM fotos_usuarios WHERE id_usuario=(SELECT id_usuario FROM usuarios WHERE email=?)");
      try
      {
        stmt_2.setString(1,email);
	stmt_2.executeUpdate();
      }
      finally
      {
        stmt_2.close();
      }

      PreparedStatement stmt_3 = conexion.prepareStatement("DELETE FROM usuarios WHERE email=?");
      try
      {
        stmt_3.setString(1,email);
	stmt_3.executeUpdate();
      }
      finally
      {
        stmt_3.close();
      }
      conexion.commit();
    }
    catch (Exception e)
    {
      conexion.rollback();
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
    finally
    {
      conexion.setAutoCommit(true);
      conexion.close();
    }
    return Response.ok().build();
  }

  @POST
  @Path("alta_articulo")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response altaArticulo(String json) throws Exception
  {
    ParamAltaArticulo p = (ParamAltaArticulo) j.fromJson(json, ParamAltaArticulo.class);
    // Validaciones básicas
    if (p.nombre == null || p.nombre.trim().isEmpty())
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el nombre del artículo"))).build();
    if (p.descripcion == null || p.descripcion.trim().isEmpty())
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar la descripción del artículo"))).build();
    if (p.precio == null || p.precio <= 0)
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar un precio válido"))).build();
    if (p.cantidad == null || p.cantidad < 0)
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar una cantidad válida"))).build();
    if (p.id_usuario == null || p.token == null)
      return Response.status(400).entity(j.toJson(new Error("Faltan parámetros de autenticación"))).build();

    Connection conexion = pool.getConnection();
    try {
      // Verificar token
      PreparedStatement stmtToken = conexion.prepareStatement("SELECT token FROM usuarios WHERE id_usuario=?");
      try {
        stmtToken.setInt(1, p.id_usuario);
        ResultSet rs = stmtToken.executeQuery();
        try {
          if (!rs.next() || !p.token.equals(rs.getString(1))) {
            return Response.status(401).entity(j.toJson(new Error("Token inválido"))).build();
          }
        } finally { rs.close(); }
      } finally { stmtToken.close(); }

      conexion.setAutoCommit(false);
      // Insertar en stock
      PreparedStatement stmtStock = conexion.prepareStatement(
        "INSERT INTO stock(id_articulo, nombre, descripcion, precio, cantidad) VALUES (0, ?, ?, ?, ?)",
        Statement.RETURN_GENERATED_KEYS);
      int idArticulo = -1;
      try {
        stmtStock.setString(1, p.nombre);
        stmtStock.setString(2, p.descripcion);
        stmtStock.setDouble(3, p.precio);
        stmtStock.setInt(4, p.cantidad);
        stmtStock.executeUpdate();
        ResultSet rs = stmtStock.getGeneratedKeys();
        if (rs.next()) {
          idArticulo = rs.getInt(1);
        }
        rs.close();
      } finally { stmtStock.close(); }

      // Insertar foto si existe
      if (p.foto != null && idArticulo > 0) {
        PreparedStatement stmtFoto = conexion.prepareStatement(
          "INSERT INTO fotos_articulos(id_foto, foto, id_articulo) VALUES (0, ?, ?)" );
        try {
          stmtFoto.setBytes(1, p.foto);
          stmtFoto.setInt(2, idArticulo);
          stmtFoto.executeUpdate();
        } finally { stmtFoto.close(); }
      }
      conexion.commit();
    } catch (Exception e) {
      conexion.rollback();
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    } finally {
      conexion.setAutoCommit(true);
      conexion.close();
    }
    return Response.ok().build();
  }

  @POST
  @Path("consulta_articulos")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response consultaArticulos(String json) throws Exception {
    ParamConsultaArticulos p = (ParamConsultaArticulos) j.fromJson(json, ParamConsultaArticulos.class);
    if (p.palabra_clave == null || p.palabra_clave.trim().isEmpty())
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar la palabra clave"))).build();
    if (p.id_usuario == null || p.token == null)
      return Response.status(400).entity(j.toJson(new Error("Faltan parámetros de autenticación"))).build();

    Connection conexion = pool.getConnection();
    try {
      // Verificar token
      PreparedStatement stmtToken = conexion.prepareStatement("SELECT token FROM usuarios WHERE id_usuario=?");
      try {
        stmtToken.setInt(1, p.id_usuario);
        ResultSet rs = stmtToken.executeQuery();
        try {
          if (!rs.next() || !p.token.equals(rs.getString(1))) {
            return Response.status(401).entity(j.toJson(new Error("Token inválido"))).build();
          }
        } finally { rs.close(); }
      } finally { stmtToken.close(); }

      // Buscar artículos
      PreparedStatement stmt = conexion.prepareStatement(
        "SELECT s.id_articulo, f.foto, s.nombre, s.descripcion, s.precio " +
        "FROM stock s LEFT OUTER JOIN fotos_articulos f ON s.id_articulo = f.id_articulo " +
        "WHERE s.nombre LIKE ? OR s.descripcion LIKE ?");
      try {
        String like = "%" + p.palabra_clave + "%";
        stmt.setString(1, like);
        stmt.setString(2, like);
        ResultSet rs = stmt.executeQuery();
        ArrayList<Object> articulos = new ArrayList<>();
        while (rs.next()) {
          java.util.HashMap<String, Object> art = new java.util.HashMap<>();
          art.put("id_articulo", rs.getInt(1));
          art.put("foto", rs.getBytes(2));
          art.put("nombre", rs.getString(3));
          art.put("descripcion", rs.getString(4));
          art.put("precio", rs.getDouble(5));
          articulos.add(art);
        }
        rs.close();
        return Response.ok().entity(j.toJson(articulos)).build();
      } finally { stmt.close(); }
    } catch (Exception e) {
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    } finally {
      conexion.close();
    }
  }

  @POST
  @Path("compra_articulo")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response compraArticulo(String json) throws Exception {
    ParamCompraArticulo p = (ParamCompraArticulo) j.fromJson(json, ParamCompraArticulo.class);
    if (p.id_articulo == null || p.cantidad == null || p.id_usuario == null || p.token == null)
      return Response.status(400).entity(j.toJson(new Error("Faltan parámetros"))).build();
    if (p.cantidad <= 0)
      return Response.status(400).entity(j.toJson(new Error("Cantidad inválida"))).build();

    Connection conexion = pool.getConnection();
    try {
      // Verificar token
      PreparedStatement stmtToken = conexion.prepareStatement("SELECT token FROM usuarios WHERE id_usuario=?");
      try {
        stmtToken.setInt(1, p.id_usuario);
        ResultSet rs = stmtToken.executeQuery();
        try {
          if (!rs.next() || !p.token.equals(rs.getString(1))) {
            return Response.status(401).entity(j.toJson(new Error("Token inválido"))).build();
          }
        } finally { rs.close(); }
      } finally { stmtToken.close(); }

      // Verificar stock
      PreparedStatement stmtStock = conexion.prepareStatement("SELECT cantidad FROM stock WHERE id_articulo=?");
      int cantidadStock = 0;
      try {
        stmtStock.setInt(1, p.id_articulo);
        ResultSet rs = stmtStock.executeQuery();
        if (rs.next()) {
          cantidadStock = rs.getInt(1);
        } else {
          return Response.status(400).entity(j.toJson(new Error("Artículo no encontrado"))).build();
        }
        rs.close();
      } finally { stmtStock.close(); }

      if (p.cantidad > cantidadStock) {
        return Response.status(400).entity(j.toJson(new Error("No hay suficientes artículos"))).build();
      }

      conexion.setAutoCommit(false);
      try {
        // Insertar o actualizar en carrito_compra
        PreparedStatement stmtCarrito = conexion.prepareStatement(
          "INSERT INTO carrito_compra(id_usuario, id_articulo, cantidad) VALUES (?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE cantidad = cantidad + ?");
        try {
          stmtCarrito.setInt(1, p.id_usuario);
          stmtCarrito.setInt(2, p.id_articulo);
          stmtCarrito.setInt(3, p.cantidad);
          stmtCarrito.setInt(4, p.cantidad);
          stmtCarrito.executeUpdate();
        } finally { stmtCarrito.close(); }

        // Actualizar stock
        PreparedStatement stmtUpdateStock = conexion.prepareStatement(
          "UPDATE stock SET cantidad = cantidad - ? WHERE id_articulo = ?");
        try {
          stmtUpdateStock.setInt(1, p.cantidad);
          stmtUpdateStock.setInt(2, p.id_articulo);
          stmtUpdateStock.executeUpdate();
        } finally { stmtUpdateStock.close(); }

        conexion.commit();
      } catch (Exception e) {
        conexion.rollback();
        return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
      } finally {
        conexion.setAutoCommit(true);
      }
    } finally {
      conexion.close();
    }
    return Response.ok().build();
  }

  @POST
  @Path("elimina_articulo_carrito_compra")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response eliminaArticuloCarritoCompra(String json) throws Exception {
    ParamEliminaArticuloCarrito p = (ParamEliminaArticuloCarrito) j.fromJson(json, ParamEliminaArticuloCarrito.class);
    if (p.id_usuario == null || p.id_articulo == null || p.token == null)
      return Response.status(400).entity(j.toJson(new Error("Faltan parámetros"))).build();

    Connection conexion = pool.getConnection();
    try {
      // Verificar token
      PreparedStatement stmtToken = conexion.prepareStatement("SELECT token FROM usuarios WHERE id_usuario=?");
      try {
        stmtToken.setInt(1, p.id_usuario);
        ResultSet rs = stmtToken.executeQuery();
        try {
          if (!rs.next() || !p.token.equals(rs.getString(1))) {
            return Response.status(401).entity(j.toJson(new Error("Token inválido"))).build();
          }
        } finally { rs.close(); }
      } finally { stmtToken.close(); }

      conexion.setAutoCommit(false);
      try {
        // Obtener cantidad a regresar al stock
        int cantidad = 0;
        PreparedStatement stmtCantidad = conexion.prepareStatement(
          "SELECT cantidad FROM carrito_compra WHERE id_usuario=? AND id_articulo=?");
        try {
          stmtCantidad.setInt(1, p.id_usuario);
          stmtCantidad.setInt(2, p.id_articulo);
          ResultSet rs = stmtCantidad.executeQuery();
          if (rs.next()) {
            cantidad = rs.getInt(1);
          } else {
            return Response.status(400).entity(j.toJson(new Error("Artículo no encontrado en el carrito"))).build();
          }
          rs.close();
        } finally { stmtCantidad.close(); }

        // Actualizar stock
        PreparedStatement stmtStock = conexion.prepareStatement(
          "UPDATE stock SET cantidad = cantidad + ? WHERE id_articulo = ?");
        try {
          stmtStock.setInt(1, cantidad);
          stmtStock.setInt(2, p.id_articulo);
          stmtStock.executeUpdate();
        } finally { stmtStock.close(); }

        // Eliminar del carrito
        PreparedStatement stmtDelete = conexion.prepareStatement(
          "DELETE FROM carrito_compra WHERE id_usuario=? AND id_articulo=?");
        try {
          stmtDelete.setInt(1, p.id_usuario);
          stmtDelete.setInt(2, p.id_articulo);
          stmtDelete.executeUpdate();
        } finally { stmtDelete.close(); }

        conexion.commit();
      } catch (Exception e) {
        conexion.rollback();
        return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
      } finally {
        conexion.setAutoCommit(true);
      }
    } finally {
      conexion.close();
    }
    return Response.ok().build();
  }

  @POST
  @Path("elimina_carrito_compra")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response eliminaCarritoCompra(String json) throws Exception {
    ParamEliminaCarrito p = (ParamEliminaCarrito) j.fromJson(json, ParamEliminaCarrito.class);
    if (p.id_usuario == null || p.token == null)
      return Response.status(400).entity(j.toJson(new Error("Faltan parámetros"))).build();

    Connection conexion = pool.getConnection();
    try {
      // Verificar token
      PreparedStatement stmtToken = conexion.prepareStatement("SELECT token FROM usuarios WHERE id_usuario=?");
      try {
        stmtToken.setInt(1, p.id_usuario);
        ResultSet rs = stmtToken.executeQuery();
        try {
          if (!rs.next() || !p.token.equals(rs.getString(1))) {
            return Response.status(401).entity(j.toJson(new Error("Token inválido"))).build();
          }
        } finally { rs.close(); }
      } finally { stmtToken.close(); }

      conexion.setAutoCommit(false);
      try {
        // Obtener todos los artículos y cantidades del carrito
        PreparedStatement stmtCarrito = conexion.prepareStatement(
          "SELECT id_articulo, cantidad FROM carrito_compra WHERE id_usuario=?");
        java.util.Map<Integer, Integer> articulos = new java.util.HashMap<>();
        try {
          stmtCarrito.setInt(1, p.id_usuario);
          ResultSet rs = stmtCarrito.executeQuery();
          while (rs.next()) {
            articulos.put(rs.getInt(1), rs.getInt(2));
          }
          rs.close();
        } finally { stmtCarrito.close(); }

        // Sumar cantidades al stock
        for (java.util.Map.Entry<Integer, Integer> entry : articulos.entrySet()) {
          PreparedStatement stmtStock = conexion.prepareStatement(
            "UPDATE stock SET cantidad = cantidad + ? WHERE id_articulo = ?");
          try {
            stmtStock.setInt(1, entry.getValue());
            stmtStock.setInt(2, entry.getKey());
            stmtStock.executeUpdate();
          } finally { stmtStock.close(); }
        }

        // Borrar todos los registros del carrito
        PreparedStatement stmtDelete = conexion.prepareStatement(
          "DELETE FROM carrito_compra WHERE id_usuario=?");
        try {
          stmtDelete.setInt(1, p.id_usuario);
          stmtDelete.executeUpdate();
        } finally { stmtDelete.close(); }

        conexion.commit();
      } catch (Exception e) {
        conexion.rollback();
        return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
      } finally {
        conexion.setAutoCommit(true);
      }
    } finally {
      conexion.close();
    }
    return Response.ok().build();
  }

  @POST
  @Path("login")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response login(String json) throws Exception {
    ParamLogin p = (ParamLogin) j.fromJson(json, ParamLogin.class);
    if (p.email == null || p.password == null)
      return Response.status(400).entity(j.toJson(new Error("Faltan parámetros"))).build();

    Connection conexion = pool.getConnection();
    try {
      PreparedStatement stmt = conexion.prepareStatement(
        "SELECT id_usuario FROM usuarios WHERE email=? AND password=?");
      int id_usuario = -1;
      try {
        stmt.setString(1, p.email);
        stmt.setString(2, p.password);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
          id_usuario = rs.getInt(1);
        }
        rs.close();
      } finally { stmt.close(); }

      if (id_usuario == -1) {
        // No encontrado
        return Response.ok().entity("").build();
      }

      // Generar token aleatorio de 20 caracteres
      String token = java.util.UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20);

      // Actualizar token en la base de datos
      PreparedStatement stmtUpdate = conexion.prepareStatement(
        "UPDATE usuarios SET token=? WHERE id_usuario=?");
      try {
        stmtUpdate.setString(1, token);
        stmtUpdate.setInt(2, id_usuario);
        stmtUpdate.executeUpdate();
      } finally { stmtUpdate.close(); }

      // Regresar id_usuario y token
      java.util.HashMap<String, Object> result = new java.util.HashMap<>();
      result.put("id_usuario", id_usuario);
      result.put("token", token);
      return Response.ok().entity(j.toJson(result)).build();
    } finally {
      conexion.close();
    }
  }
}