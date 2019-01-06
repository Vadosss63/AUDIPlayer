package com.gmail.parusovvadim.audioplayer;

public class Folder implements NodeDirectory
{
    private String name;
    private int number;
    private int parentNumber;
    private int numberTracks;
    private boolean m_isFolderUp = false;
    private String m_path;

    Folder(String name)
    {
        this.name = name;
    }

    void setPath(String path) {
        this.m_path = path;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    void setParentNumber(int parentNumber) {
        this.parentNumber = parentNumber;
    }

    void setNumberTracks(int numberTracks) {
        this.numberTracks = numberTracks;
    }

    void setIsFolderUp(boolean isFolderUp) {
        this.m_isFolderUp = isFolderUp;
    }


    @Override
    public void SetName(String name)
    {
        this.name = name;
    }

    @Override
    public String GetName()
    {
        return name;
    }

    @Override
    public String GetPathDir() {
        return m_path;
    }

    @Override
    public int GetNumber()
    {
        return number;
    }

    @Override
    public int GetParentNumber()
    {
        return parentNumber;
    }

    @Override
    public int GetNumberTracks()
    {
        return numberTracks;
    }

    @Override
    public boolean IsFolder()
    {
        return true;
    }

    @Override
    public boolean IsFolderUp() {
        return m_isFolderUp;
    }
}
