package yxy.neatnotebook.service;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import yxy.neatnotebook.domain.Ebook;
import yxy.neatnotebook.domain.EbookExample;
import yxy.neatnotebook.mapper.EbookMapper;
import yxy.neatnotebook.req.EbookReq;
import yxy.neatnotebook.resp.EbookResp;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class EbookService {
    @Resource
    private EbookMapper ebookMapper;

    public List<EbookResp> list(EbookReq req){
        EbookExample ebookExample = new EbookExample();
        EbookExample.Criteria criteria = ebookExample.createCriteria();
        criteria.andNameLike("%"+ req.getName() + "%");
        List<Ebook> ebookList = ebookMapper.selectByExample(ebookExample);

        List<EbookResp> respList = new ArrayList<>();
        for(Ebook ebook: ebookList){
            EbookResp ebookResp = new EbookResp();
            BeanUtils.copyProperties(ebook, ebookResp);
            respList.add(ebookResp);
        }
        return respList;
    }
}
