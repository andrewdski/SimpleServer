/*
 * Copyright (c) 2010 SimpleServer authors (see CONTRIBUTORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package simpleserver.config;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import simpleserver.Player;
import simpleserver.options.Options;

public class AreaList extends AsciiConfig {
  private final ConcurrentLinkedQueue<Area> areas;
  private final ConcurrentHashMap<String, Area> names;
  private final ConcurrentHashMap<String, TempArea> tempAreas;
  private Options opt;

  public AreaList(Options opt) {
    super("area-list.txt");
    this.opt = opt;
    tempAreas = new ConcurrentHashMap<String, TempArea>();
    areas = new ConcurrentLinkedQueue<Area>();
    names = new ConcurrentHashMap<String, Area>();
  }

  public boolean checkArea(RectangularPrism r) {
    if ((r.x.b - r.x.a) * (r.z.b - r.z.a) > opt.getInt("protectedAreaLimit")) {
      return false;
    }
    return true;
  }

  public synchronized int createArea(String name, int x, int y, int z,
                                     boolean isGroupLock, int groupID,
                                     boolean tall) {
    name = name.toLowerCase();
    if (tempAreas.containsKey(name)) {
      if (names.containsKey(name)) {
        return -3;
      }
      if (tall) {
        y = 255;
      }
      TempArea tmp = tempAreas.remove(name);
      RectangularPrism r = new RectangularPrism(tmp.area[0], x, tmp.area[1], y,
                                                tmp.area[2], z);
      if (!checkArea(r)) {
        return -2;
      }
      Area a = new Area(name, r, isGroupLock, groupID);
      for (Iterator<Area> itr = areas.iterator(); itr.hasNext();) {
        Area b = itr.next();
        if (b.area.intersect(r)) {
          return -1;
        }
      }
      names.put(name, a);
      areas.add(a);
      save();
      return 2;
    }
    else {
      if (tall) {
        y = 0;
      }
      int[] a = { x, y, z };
      TempArea tmp = new TempArea(name, a, isGroupLock);
      tempAreas.put(name, tmp);
      return 1;
    }
  }

  private synchronized void setArea(String name, int x1, int x2, int y1,
                                    int y2, int z1, int z2,
                                    boolean isGroupLock, int groupID) {

    RectangularPrism r = new RectangularPrism(x1, x2, y1, y2, z1, z2);
    Area a = new Area(name, r, isGroupLock, groupID);
    areas.add(a);
    names.put(name, a);
  }

  public synchronized boolean resetArea(String name) {
    name = name.toLowerCase();
    return (tempAreas.remove(name) != null);

  }

  public synchronized boolean removeArea(String name) {
    name = name.toLowerCase();
    if (names.containsKey(name)) {
      Area a = names.get(name);
      areas.remove(a);
      names.remove(name);
      save();
      return true;
    }
    return false;
  }

  public String isProtected(Player p, int x, int y, int z) {
    for (Iterator<Area> itr = areas.iterator(); itr.hasNext();) {
      Area a = itr.next();
      if (a.intersect(x, y, z)) {
        String name = p.getName().toLowerCase();
        if (!a.allowed(name) || !a.allowed(p.getGroupId())) {
          return a.name;
        }
      }
    }
    return null;
  }

  @Override
  public void load() {
    areas.clear();

    super.load();
  }

  @Override
  protected void loadLine(String line) {
    line = line.trim();
    if (line.length() == 0) {
      return;
    }

    String[] tokens = line.split(",");
    if (tokens.length > 4) {
      int x1, x2;
      int y1, y2;
      int z1, z2;
      try {
        x1 = Integer.parseInt(tokens[3]);
        y1 = Integer.parseInt(tokens[5]);
        z1 = Integer.parseInt(tokens[7]);
        x2 = Integer.parseInt(tokens[4]);
        y2 = Integer.parseInt(tokens[6]);
        z2 = Integer.parseInt(tokens[8]);
      }
      catch (NumberFormatException e) {
        System.out.println("Skipping malformed area metadata: " + line);
        return;
      }

      setArea(tokens[0], x1, x2, y1, y2, z1, z2,
              Boolean.parseBoolean(tokens[1]), Integer.parseInt(tokens[2]));
    }
  }

  @Override
  protected String saveString() {
    StringBuilder output = new StringBuilder();
    for (Iterator<Area> itr = areas.iterator(); itr.hasNext();) {
      Area area = itr.next();
      output.append(area.name);
      output.append(",");
      output.append(area.isGroup);
      output.append(",");
      output.append(area.groupID);
      output.append(",");
      output.append(area.area.x.a);
      output.append(",");
      output.append(area.area.x.b);
      output.append(",");
      output.append(area.area.y.a);
      output.append(",");
      output.append(area.area.y.b);
      output.append(",");
      output.append(area.area.z.a);
      output.append(",");
      output.append(area.area.z.b);
      output.append("\n");
    }
    return output.toString();
  }

  private static final class Tuple {
    private final int a, b;

    public Tuple(int x1, int x2) {
      if (x2 > x1) {
        a = x1;
        b = x2;
      }
      else {
        a = x2;
        b = x1;
      }
    }

    public boolean intersect(int x) {
      if (x >= a && x <= b) {
        return true;
      }
      return false;
    }

    public boolean intersect(Tuple c) {
      if (c.b < a) {
        return false;
      }
      if (c.a > b) {
        return false;
      }
      return true;
    }
  }

  private static final class RectangularPrism {
    private final Tuple x;
    private final Tuple y;
    private final Tuple z;

    public RectangularPrism(int x1, int x2, int y1, int y2, int z1, int z2) {
      x = new Tuple(x1, x2);
      y = new Tuple(y1, y2);
      z = new Tuple(z1, z2);
    }

    public boolean intersect(RectangularPrism d) {
      if (x.intersect(d.x) && y.intersect(d.y) && z.intersect(d.z)) {
        return true;
      }
      return false;
    }

    public boolean intersect(int x1, int y1, int z1) {
      if (x.intersect(x1) && y.intersect(y1) && z.intersect(z1)) {
        return true;
      }
      return false;
    }
  }

  private static final class TempArea {
    @SuppressWarnings("unused")
    private final String name;
    private final int[] area;
    @SuppressWarnings("unused")
    private final boolean isGroup;

    private TempArea(String name, int[] area, boolean isGroup) {
      this.name = name;
      this.area = area;
      this.isGroup = isGroup;
    }

  }

  private static final class Area {
    private final String name;
    private final RectangularPrism area;
    private final int groupID;
    private final boolean isGroup;

    private Area(String name, RectangularPrism area, boolean isGroup,
                 int groupID) {
      this.name = name;
      this.area = area;
      this.isGroup = isGroup;
      this.groupID = groupID;
    }

    public boolean intersect(int x1, int y1, int z1) {
      return area.intersect(x1, y1, z1);
    }

    public boolean allowed(String name) {
      if (this.name.trim().equals(name)) {
        return true;
      }
      return false;
    }

    public boolean allowed(int gid) {
      if (gid == groupID && isGroup) {
        return true;
      }
      return false;
    }
  }
}
